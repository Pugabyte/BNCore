package me.pugabyte.nexus.framework.commands.models;

import eden.annotations.Disabled;
import eden.interfaces.PlayerOwnedObject;
import lombok.SneakyThrows;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.menus.MenuUtils.ConfirmationMenu;
import me.pugabyte.nexus.framework.commands.Commands;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Async;
import me.pugabyte.nexus.framework.commands.models.annotations.Confirm;
import me.pugabyte.nexus.framework.commands.models.annotations.Cooldown;
import me.pugabyte.nexus.framework.commands.models.annotations.Cooldown.Part;
import me.pugabyte.nexus.framework.commands.models.annotations.Fallback;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.Switch;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.framework.commands.models.events.CommandRunEvent;
import me.pugabyte.nexus.framework.commands.models.events.CommandTabEvent;
import me.pugabyte.nexus.framework.exceptions.NexusException;
import me.pugabyte.nexus.framework.exceptions.postconfigured.CommandCooldownException;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.framework.exceptions.postconfigured.PlayerNotFoundException;
import me.pugabyte.nexus.framework.exceptions.postconfigured.PlayerNotOnlineException;
import me.pugabyte.nexus.framework.exceptions.preconfigured.MissingArgumentException;
import me.pugabyte.nexus.framework.exceptions.preconfigured.NoPermissionException;
import me.pugabyte.nexus.models.cooldown.CooldownService;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.objenesis.ObjenesisStd;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static me.pugabyte.nexus.framework.commands.models.CustomCommand.getSwitchPattern;
import static me.pugabyte.nexus.framework.commands.models.PathParser.getLiteralWords;
import static me.pugabyte.nexus.framework.commands.models.PathParser.getPathString;
import static me.pugabyte.nexus.utils.StringUtils.COMMA_SPLIT_REGEX;
import static me.pugabyte.nexus.utils.StringUtils.asParsableDecimal;
import static me.pugabyte.nexus.utils.StringUtils.camelCase;
import static me.pugabyte.nexus.utils.Utils.getDefaultPrimitiveValue;
import static me.pugabyte.nexus.utils.Utils.getMaxValue;
import static me.pugabyte.nexus.utils.Utils.getMinValue;
import static me.pugabyte.nexus.utils.Utils.isBoolean;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;

@SuppressWarnings("unused")
public abstract class ICustomCommand {

	public void execute(CommandRunEvent event) {
		try {
			CustomCommand command = getCommand(event);
			Method method = getMethod(event);
			if (method == null)
				return;
			event.setUsage(method);
			if (!hasPermission(event.getSender(), method))
				throw new NoPermissionException();
			checkCooldown(command);
			command.invoke(method, event);
		} catch (Exception ex) {
			event.handleException(ex);
		}
	}

	public List<String> tabComplete(CommandTabEvent event) {
		try {
			getCommand(event);
			return new PathParser(event).tabComplete(event);
		} catch (Exception ex) {
			event.handleException(ex);
		}
		return new ArrayList<>();
	}

	public String getName() {
		return Commands.prettyName(this);
	}

	public List<String> getAliases() {
		List<String> aliases = new ArrayList<>();

		for (Annotation annotation : this.getClass().getAnnotations()) {
			if (annotation instanceof Aliases) {
				for (String alias : ((Aliases) annotation).value()) {
					if (!Pattern.compile("[a-zA-Z0-9_-]+").matcher(alias).matches()) {
						Nexus.warn("Alias invalid: " + getName() + "Command.java / " + alias);
						continue;
					}

					aliases.add(alias);
				}
			}
		}

		return aliases;
	}

	public List<String> getAllAliases() {
		List<String> aliases = getAliases();
		aliases.add(getName());
		return aliases;
	}

	private String _getPermission() {
		if (this.getClass().getAnnotation(Permission.class) != null)
			return this.getClass().getAnnotation(Permission.class).value();
		return null;
	}

	protected void invoke(Method method, CommandRunEvent event) {
		Runnable function = () -> {
			try {
				Object[] objects = getMethodParameters(method, event, true);
				method.setAccessible(true);
				method.invoke(this, objects);
			} catch (Exception ex) {
				event.handleException(ex);
			}
		};

		Runnable run = () -> {
			if (method.getAnnotation(Async.class) != null)
				Tasks.async(function);
			else
				function.run();
		};

		Confirm confirm = method.getAnnotation(Confirm.class);
		if (event.getSender() instanceof Player && confirm != null) {
			ConfirmationMenu.builder()
					.onConfirm(e -> run.run())
					.title(confirm.title())
					.open(event.getPlayer());
		} else
			run.run();
	}

	Object[] getMethodParameters(Method method, CommandEvent event, boolean doValidation) {
		Parameter[] allParameters = method.getParameters();

		List<Parameter> switches = new ArrayList<>();
		List<Parameter> parameters = new ArrayList<>();

		for (Parameter parameter : allParameters)
			if (parameter.getDeclaredAnnotation(Switch.class) != null)
				switches.add(parameter);
			else
				parameters.add(parameter);

		Object[] convertedSwitches = convertSwitches(method, event, doValidation, switches);
		Object[] convertedParameters = convertParameters(method, event, doValidation, parameters);

		return new ArrayList<>() {{
			addAll(Arrays.asList(convertedParameters));
			addAll(Arrays.asList(convertedSwitches));
		}}.toArray(Object[]::new);
	}

	private Object[] convertSwitches(Method method, CommandEvent event, boolean doValidation, List<Parameter> switches) {
		Object[] objects = new Object[switches.size()];

		List<String> args = new ArrayList<>(event.getArgs());

		if (args.isEmpty())
			return objects;

		int i = 0;
		for (Parameter parameter : switches) {
			Arg annotation = parameter.getDeclaredAnnotation(Arg.class);
			String defaultValue = annotation == null || isNullOrEmpty(annotation.value()) ? null : annotation.value();

			Pattern pattern = getSwitchPattern(parameter);

			boolean found = false;
			for (String arg : args) {
				Matcher matcher = pattern.matcher(arg);

				if (matcher.find()) {
					found = true;
					String group = matcher.group();
					String value = defaultValue;
					if (group.contains("="))
						value = group.split("=", 2)[1];
					if (value == null && isBoolean(parameter))
						value = "true";

					objects[i] = convert(value, null, parameter.getType(), parameter, parameter.getName(), event, false);

					event.getArgs().remove(arg);
				}
			}

			if (objects[i] == null && parameter.getType().isPrimitive())
				objects[i] = getDefaultPrimitiveValue(parameter.getType());

			if (!found && !isNullOrEmpty(defaultValue))
				objects[i] = convert(defaultValue, null, parameter.getType(), parameter, parameter.getName(), event, false);

			i++;
		}
		return objects;
	}

	private Object[] convertParameters(Method method, CommandEvent event, boolean doValidation, List<Parameter> parameters) {
		Object[] objects = new Object[parameters.size()];
		List<String> args = event.getArgs();
		String pathValue = method.getAnnotation(Path.class).value();
		Iterator<String> path = Arrays.asList(pathValue.split(" ")).iterator();

		// TODO: Validate params and path have same args

		int i = 0;
		int pathIndex = 0;
		for (Parameter parameter : parameters) {
			String pathArg = "";
			while (!pathArg.startsWith("{") && !pathArg.startsWith("[") && !pathArg.startsWith("<") && path.hasNext()) {
				pathArg = path.next();
				++pathIndex;
			}

			Arg annotation = parameter.getDeclaredAnnotation(Arg.class);
			String value = (annotation == null ? null : annotation.value());
			int contextArgIndex = (annotation == null ? -1 : annotation.context());
			Object contextArg = (contextArgIndex > 0 && objects.length >= contextArgIndex) ? objects[contextArgIndex - 1] : null;

			if (args.size() >= pathIndex) {
				if (annotation == null || isNullOrEmpty(annotation.permission()) || event.getSender().hasPermission(annotation.permission()))
					if (pathArg.contains("..."))
						value = String.join(" ", args.subList(pathIndex - 1, args.size()));
					else
						value = args.get(pathIndex - 1);
			}

			boolean required = doValidation && (pathArg.startsWith("<") || (pathArg.startsWith("[") && !isNullOrEmpty(value)));
			Object converted = convert(value, contextArg, parameter.getType(), parameter, pathArg.substring(1, pathArg.length() - 1), event, required);
			if (required && converted == null)
				throw new MissingArgumentException();
			objects[i++] = converted;
		}
		return objects;
	}

	private static final List<Class<? extends Exception>> conversionExceptions = Arrays.asList(
			InvalidInputException.class,
			PlayerNotFoundException.class,
			PlayerNotOnlineException.class
	);

	@SneakyThrows
	private Object convert(String value, Object context, Class<?> type, Parameter parameter, String name, CommandEvent event, boolean required) {
		Arg annotation = parameter.getDeclaredAnnotation(Arg.class);

		double argMinDefault = (Double) Arg.class.getDeclaredMethod("min").getDefaultValue();
		double argMaxDefault = (Double) Arg.class.getDeclaredMethod("max").getDefaultValue();

		if (annotation != null) {
			if (annotation.regex().length() > 0)
				if (!value.matches(annotation.regex()))
					throw new InvalidInputException(camelCase(name) + " must match regex " + annotation.regex());

			if (!isNumber(type))
				if (isNullOrEmpty(annotation.minMaxBypass()) || !event.getSender().hasPermission(annotation.minMaxBypass()))
					if (value.length() < annotation.min() || value.length() > annotation.max()) {
						DecimalFormat formatter = StringUtils.getFormatter(Integer.class);
						String min = formatter.format(annotation.min());
						String max = formatter.format(annotation.max());
						double minDefault = (Double) Arg.class.getDeclaredMethod("min").getDefaultValue();
						double maxDefault = (Double) Arg.class.getDeclaredMethod("max").getDefaultValue();

						String error = camelCase(name) + " length must be ";
						if (annotation.min() == minDefault && annotation.max() != maxDefault)
							throw new InvalidInputException(error + "&e" + max + " &ccharacters or shorter");
						else if (annotation.min() != minDefault && annotation.max() == maxDefault)
							throw new InvalidInputException(error + "&e" + min + " &ccharacters or longer");
						else
							throw new InvalidInputException(error + "between &e" + min + " &cand &e" + max + " &ccharacters");
					}
		}

		if (Collection.class.isAssignableFrom(type)) {
			if (annotation == null)
				throw new InvalidInputException("Collection parameter must define concrete type with @Arg");

			List<Object> values = new ArrayList<>();
			for (String index : value.split(COMMA_SPLIT_REGEX))
				values.add(convert(index, context, annotation.type(), parameter, name, event, required));
			values.removeIf(Objects::isNull);
			return values;
		}

		try {
			CustomCommand command = event.getCommand();
			if (Commands.getConverters().containsKey(type)) {
				Method converter = Commands.getConverters().get(type);
				boolean isAbstract = Modifier.isAbstract(converter.getDeclaringClass().getModifiers());
				if (!(isAbstract || converter.getDeclaringClass().equals(command.getClass())))
					command = getNewCommand(command.getEvent(), converter.getDeclaringClass());
				if (converter.getParameterCount() == 1)
					return converter.invoke(command, value);
				else if (converter.getParameterCount() == 2)
					return converter.invoke(command, value, context);
				else
					throw new NexusException("Unknown converter parameters in " + converter.getName());
			} else if (type.isEnum()) {
				return convertToEnum(value, (Class<? extends Enum<?>>) type);
			} else if (PlayerOwnedObject.class.isAssignableFrom(type)) {
				return convertToPlayerOwnedObject(value, (Class<? extends PlayerOwnedObject>) type);
			}
		} catch (InvocationTargetException ex) {
			if (Nexus.isDebug())
				ex.printStackTrace();
			if (required)
				if (!isNullOrEmpty(value) && conversionExceptions.contains(ex.getCause().getClass()))
					throw ex;
				else
					throw new MissingArgumentException();
			else
				return null;
		}

		if (isNullOrEmpty(value))
			if (required)
				throw new MissingArgumentException();
			else
				if (type.isPrimitive())
					return getDefaultPrimitiveValue(type);
				else
					return null;

		if (Boolean.class == type || Boolean.TYPE == type) {
			if (Arrays.asList("enable", "on", "yes", "1").contains(value)) value = "true";
			return Boolean.parseBoolean(value);
		}
		try {
			Number number = null;
			if (Integer.class == type || Integer.TYPE == type) number = Integer.parseInt(value);
			if (Double.class == type || Double.TYPE == type) number = Double.parseDouble(value);
			if (Float.class == type || Float.TYPE == type) number = Float.parseFloat(value);
			if (Short.class == type || Short.TYPE == type) number = Short.parseShort(value);
			if (Long.class == type || Long.TYPE == type) number = Long.parseLong(value);
			if (Byte.class == type || Byte.TYPE == type) number = Byte.parseByte(value);
			if (BigDecimal.class == type) number = BigDecimal.valueOf(Double.parseDouble(asParsableDecimal(value)));

			if (number != null) {
				if (annotation != null) {
					if (isNullOrEmpty(annotation.minMaxBypass()) || !event.getSender().hasPermission(annotation.minMaxBypass())) {
						double annotationDefaultMin = (Double) Arg.class.getDeclaredMethod("min").getDefaultValue();
						double annotationDefaultMax = (Double) Arg.class.getDeclaredMethod("max").getDefaultValue();

						double annotationConfiguredMin = annotation.min();
						double annotationConfiguredMax = annotation.max();

						Number classDefaultMin = getMinValue(type);
						Number classDefaultMax = getMaxValue(type);

						BigDecimal min = (annotationConfiguredMin != annotationDefaultMin ? BigDecimal.valueOf(annotationConfiguredMin) : new BigDecimal(classDefaultMin.toString()));
						BigDecimal max = (annotationConfiguredMax != annotationDefaultMax ? BigDecimal.valueOf(annotationConfiguredMax) : new BigDecimal(classDefaultMax.toString()));

						int minComparison = BigDecimal.valueOf(number.doubleValue()).compareTo(min);
						int maxComparison = BigDecimal.valueOf(number.doubleValue()).compareTo(max);

						if (minComparison < 0 || maxComparison > 0) {
							DecimalFormat formatter = StringUtils.getFormatter(type);

							boolean usingDefaultMin = annotationDefaultMin == annotationConfiguredMin;
							boolean usingDefaultMax = annotationDefaultMax == annotationConfiguredMax;

							String minFormatted = formatter.format(annotation.min());
							String maxFormatted = formatter.format(annotation.max());

							String error = camelCase(name) + " must be ";
							if (usingDefaultMin && !usingDefaultMax)
								throw new InvalidInputException(error + "&e" + maxFormatted + " &cor less");
							else if (!usingDefaultMin && usingDefaultMax)
								throw new InvalidInputException(error + "&e" + minFormatted + " &cor greater");
							else
								throw new InvalidInputException(error + "between &e" + minFormatted + " &cand &e" + maxFormatted);
						}
					}
				}

				return number;
			}
		} catch (NumberFormatException ex) {
			throw new InvalidInputException("&e" + value + " &cis not a valid " + (type == BigDecimal.class ? "number" : type.getSimpleName().toLowerCase()));
		}
		return value;
	}

	private boolean isNumber(Class<?> type) {
		return Integer.class == type || Integer.TYPE == type ||
				Double.class == type || Double.TYPE == type ||
				Float.class == type || Float.TYPE == type ||
				Short.class == type || Short.TYPE == type ||
				Long.class == type || Long.TYPE == type ||
				Byte.class == type || Byte.TYPE == type ||
				BigDecimal.class == type;
	}

	@SneakyThrows
	private CustomCommand getCommand(CommandEvent event) {
		Constructor<? extends CustomCommand> constructor = event.getCommand().getClass().getDeclaredConstructor(CommandEvent.class);
		constructor.setAccessible(true);
		CustomCommand command = constructor.newInstance(event);
		event.setCommand(command);
		return command;
	}

	@SneakyThrows
	CustomCommand getNewCommand(CommandEvent originalEvent, Class<?> clazz) {
		CustomCommand customCommand = new ObjenesisStd().newInstance((Class<? extends CustomCommand>) clazz);
		CommandRunEvent newEvent = new CommandRunEvent(originalEvent.getSender(), customCommand, customCommand.getName(), new ArrayList<>(), new ArrayList<>());
		return getCommand(newEvent);
	}

	List<Method> getPathMethods(CommandEvent event) {
		List<Method> methods = getPathMethods();

		Map<String, Method> overridden = new HashMap<>();
		methods.forEach(method -> {
			String key = method.getName() + "(" + Arrays.stream(method.getParameterTypes()).map(Class::getName).collect(Collectors.joining(",")) + ")";
			if (!overridden.containsKey(key))
				overridden.put(key, method);
			else if (overridden.get(key).getDeclaringClass().isAssignableFrom(method.getDeclaringClass()))
				overridden.put(key, method);
		});

		methods.clear();
		methods.addAll(overridden.values());

		methods.sort(
				Comparator.comparing(method ->
						Arrays.stream(getLiteralWords(getPathString((Method) method)).split(" "))
								.filter(path -> !isNullOrEmpty(path))
								.count())
				.thenComparing(method ->
						Arrays.stream(getPathString((Method) method).split(" "))
								.filter(path -> !isNullOrEmpty(path))
								.count()));

		List<Method> filtered = methods.stream()
			.filter(method -> method.getAnnotation(Disabled.class) == null)
			.filter(method -> hasPermission(event.getSender(), method))
			.collect(Collectors.toList());

		if (methods.size() > 0 && filtered.size() == 0)
			throw new NoPermissionException();

		return filtered;
	}

	@NotNull
	public List<Method> getPathMethods() {
		return new ArrayList<>(getAllMethods(this.getClass(), withAnnotation(Path.class)));
	}

	private Method getMethod(CommandRunEvent event) {
		Method method = new PathParser(event).match(event.getArgs());

		if (method == null) {
			Fallback fallback = event.getCommand().getClass().getAnnotation(Fallback.class);
			if (fallback != null)
				PlayerUtils.runCommand(event.getSender(), fallback.value() + ":" + event.getAliasUsed() + " " + event.getArgsString());
			else if (!event.getArgsString().equalsIgnoreCase("help"))
				PlayerUtils.runCommand(event.getSender(), event.getAliasUsed() + " help");
			else
				throw new InvalidInputException("No matching path");
		}

		return method;
	}

	boolean hasPermission(CommandSender sender, Method method) {
		String permission = _getPermission();

		if (permission != null && !sender.hasPermission(permission))
			return false;

		if (method.isAnnotationPresent(Permission.class)) {
			Permission pathPermission = method.getAnnotation(Permission.class);
			if (permission != null)
				permission = (pathPermission.absolute() ? "" : (permission + ".")) + pathPermission.value();
			else
				permission = pathPermission.value();

			return sender.hasPermission(permission);
		}

		return true;
	}

	private void checkCooldown(CustomCommand command) {
		Method method = ((CommandRunEvent) command.getEvent()).getMethod();
		checkCooldown(command, command.getClass().getAnnotation(Cooldown.class), command.getName());
		checkCooldown(command, method.getAnnotation(Cooldown.class), command.getName() + "#" + method.getName());
	}

	private void checkCooldown(CustomCommand command, Cooldown cooldown, String commandId) {
		if (cooldown != null) {
			boolean bypass = false;
			if (!(command.getEvent().getSender() instanceof Player))
				bypass = true;
			else if (cooldown.bypass().length() > 0 && command.getEvent().getPlayer().hasPermission(cooldown.bypass()))
				bypass = true;

			if (!bypass) {
				int ticks = 0;
				for (Part part : cooldown.value())
					ticks += part.value().get() * part.x();

				CooldownService service = new CooldownService();
				UUID uuid = cooldown.global() ? StringUtils.getUUID0() : ((Player) command.getEvent().getSender()).getUniqueId();
				String type = "command:" + commandId;

				if (!service.check(uuid, type, ticks))
					throw new CommandCooldownException(uuid, type);
			}
		}
	}

	protected abstract PlayerOwnedObject convertToPlayerOwnedObject(String value, Class<? extends PlayerOwnedObject> type);

	@SneakyThrows
	protected Enum<?> convertToEnum(String filter, Class<? extends Enum<?>> clazz) {
		if (filter == null) throw new InvocationTargetException(new NexusException("Missing argument"));
		return Arrays.stream(clazz.getEnumConstants())
				.filter(value -> value.name().equalsIgnoreCase(filter))
				.findFirst()
				.orElseThrow(() -> new InvalidInputException(clazz.getSimpleName() + " from " + filter + " not found"));
	}

	protected List<String> tabCompleteEnum(String filter, Class<? extends Enum<?>> clazz) {
		return Arrays.stream(clazz.getEnumConstants())
				.map(value -> value.name().toLowerCase())
				.filter(value -> value.toLowerCase().startsWith(filter.toLowerCase()))
				.collect(Collectors.toList());
	}

}



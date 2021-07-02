package me.pugabyte.nexus.framework.persistence.serializer.mongodb;

import dev.morphia.converters.SimpleValueConverter;
import dev.morphia.converters.TypeConverter;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import lombok.NoArgsConstructor;
import me.pugabyte.nexus.models.chat.PrivateChannel;

import java.util.List;

@NoArgsConstructor
public class PrivateChannelConverter extends TypeConverter implements SimpleValueConverter {

	public PrivateChannelConverter(Mapper mapper) {
		super(PrivateChannel.class);
	}

	@Override
	public Object encode(Object value, MappedField optionalExtraInfo) {
		if (value == null) return null;

		return ((PrivateChannel) value).getRecipients().stream()
			.map(chatter -> chatter.getUuid().toString())
			.toList();
	}

	@Override
	public Object decode(Class<?> aClass, Object value, MappedField mappedField) {
		if (value == null) return null;
		return new PrivateChannel((List<String>) value);
	}

}

package me.whereareiam.identica.cache.codec.type;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonCodecTest {
	@Test
	void roundTripWithConfiguraJson() {
		JsonCodec<Sample> codec = JsonCodec.of(Sample.class);
		Sample sample = new Sample("alpha", 42);

		byte[] data = codec.encode(sample);
		Sample restored = codec.decode(data);

		assertEquals(sample, restored);
	}

	@NoArgsConstructor
	@AllArgsConstructor
	private static final class Sample {
		private String name;
		private int number;

		@Override
		public boolean equals(Object other) {
			if (this == other) return true;
			if (other == null || getClass() != other.getClass())
				return false;

			Sample sample = (Sample) other;
			return number == sample.number && (Objects.equals(name, sample.name));
		}

		@Override
		public int hashCode() {
			int result = name != null
					? name.hashCode()
					: 0;

			return 31 * result + number;
		}
	}
}


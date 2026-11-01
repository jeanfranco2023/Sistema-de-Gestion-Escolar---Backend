package com.colegio.shuji.matricula.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReniecPkiHttpSourceCacheTest {
  @Test
  void devuelveBytesDeCachePublicaVigenteSinConsultarLaRed() throws Exception {
    var source = new ReniecPkiHttpSource(true);
    String url = "https://www.reniec.gob.pe/archivo-prueba.crl";
    byte[] contenido = {1, 2, 3};

    Class<?> cacheType = Class.forName(ReniecPkiHttpSource.class.getName() + "$Cache");
    Constructor<?> constructor = cacheType.getDeclaredConstructor(byte[].class, Instant.class);
    constructor.setAccessible(true);
    Object entrada = constructor.newInstance(contenido, Instant.now().plusSeconds(60));
    Field cacheField = ReniecPkiHttpSource.class.getDeclaredField("cache");
    cacheField.setAccessible(true);
    cache(cacheField, source).put(url, entrada);

    assertArrayEquals(contenido, source.descargar(url).orElseThrow());
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> cache(Field field, ReniecPkiHttpSource source) throws IllegalAccessException {
    return (Map<String, Object>) field.get(source);
  }
}

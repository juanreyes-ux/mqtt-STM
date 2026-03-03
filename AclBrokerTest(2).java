

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.Test;

/*
 *  TEST PARA ACL DE LOS BROKERS STM
 *  SOLO SE EJECUTA PARA EL BROKER[0] "DEV", CAMBIAR EL ORDEN DEL ARRAY ...
 *  SE VERIFICA EL PRODUCTO CARTESIANO DE USUARIOS Y TÓPICOS CON SU RESPECTIVO RESULTADO ESPERADO
 */

public class AclBrokerTest {
  private MqttClient client;
  private static final String[] BROKERS = {
    "tcp://127.0.0.1:1883",
    //  "tcp://broker-stm-dev.apps.ocp4-desa.imm.gub.uy:1883",
    // "tcp://broker-stm-stag.apps.ocp4-desa.imm.gub.uy:1883",
    // NO usar !!! "tcp://broker-stm-preprod.apps.ocp4-prod.imm.gub.uy:1883",
    // NO usar !!! "tcp://broker-stm-prod.apps.ocp4-prod.imm.gub.uy:1883"
  };

  private static final int TIMEOUT = 2;

  // Configuración de credenciales para cada broker
  public MqttConnectionOptions getOptions(String brokerId, String username) {
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setUserName(username);
    switch (username) {
      case "stm-admin":
        options.setPassword("UNeJh6LDSITdksMhEQshFw==".getBytes());
        break;
      case "stm":
        options.setPassword("1tg9n8Ma74S80VYJHp+8yQ==".getBytes());
        break;
      case "mebilor":
        options.setPassword("3POD7XlNyuXga5CT8e5RwA==".getBytes());
        break;
      case "lstm":
        options.setPassword("5QpyhuoYC3kmwrv/OgS/8g==".getBytes());
        break;
      case "ciemsa":
        options.setPassword("fHrudaQZrq6rBt2+3B5bUQ==".getBytes());
        break;
      case "cutcsa":
        options.setPassword("AlhL+B4krALE2bhkuQRyQQ==".getBytes());
        break;
      case "coetc":
        options.setPassword("DKWsqXVkav0TAtCg+8zJtg==".getBytes());
        break;
      case "come":
        options.setPassword("kI6pkaU8+b85cQnG+ABAEQ==".getBytes());
        break;
      case "ucot":
        options.setPassword("aP4+NTxjjTh/ePQBmEIn4w==".getBytes());
        break;
      case "casanova":
        options.setPassword("BovwWIJT+3WpOIBluyGkLw==".getBytes());
        break;
      case "copsa":
        options.setPassword("wtiMZL8SWcma0SFjCvOCWA==".getBytes());
        break;
      case "zeballos":
        options.setPassword("x/6Y9xSrrlQ31c+rZSm1EA==".getBytes());
        break;
      case "cita":
        options.setPassword("vk3HY2JNXm3mBgu4DtMsGg==".getBytes());
        break;
      case "TalaPando":
        options.setPassword("/tuPL2lIvNzGrVF9ORqO2A==".getBytes());
        break;
      case "coDelEste":
        options.setPassword("BdwsKO20OXJiqZT3S9EwKQ==".getBytes());
        break;
      case "rutasDelNorte":
        options.setPassword("10I06s3s5qUAdBfOPOCSMQ==".getBytes());
        break;
      case "satt":
        options.setPassword("bsCJx6Ia/ZSwZFTBvEaO2A==".getBytes());
        break;
      case "STM":
        options.setPassword("Jar11y".getBytes());
        break;
      default:
        options.setPassword("default".getBytes());
        break;
    }
 options.setCleanStart(true);
    options.setSessionExpiryInterval((long) 10);
    options.setConnectionTimeout(30);
    options.setKeepAliveInterval(60);
    return options;
  }

  private void testOperation(
      String brokerUrl,
      String username,
      String operationType,
      String topic,
      String message,
      boolean expectedResult)
      throws Exception {

    String clientId = "TEST";
    client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
    String brokerId = extractBrokerId(brokerUrl);
    MqttConnectionOptions options = getOptions(brokerId, username);

    try {
      client.connect(options);
    } catch (MqttException e) {
      if (expectedResult) { // Solo en este caso es un error
        throw new RuntimeException(
            operationType.toUpperCase() + " Falló en Connect: " + e.getMessage());
      }
    }

    try {
      if ("publish".equals(operationType)) {
        testPublish(topic, message, expectedResult);
      } else if ("subscribe".equals(operationType)) {
        testSubscribe(topic, message, expectedResult);
      }
    } catch (MqttException e) {
      // Ignorar errores que ocurren durante las pruebas
    } finally {
      try {
        if (client.isConnected()) {
          client.disconnect();
        }
      } catch (MqttException e) {
        // Ignorar errores de desconexión
      }
    }
  }

  private void testPublish(String topic, String message, boolean expectedResult) {
    MqttMessage mqttMessage = new MqttMessage(message.getBytes());
    mqttMessage.setQos(2);
    boolean publishSuccess = false;
    boolean exceptionCaught = false;
    String exceptionMessage = "";
    try {
      client.publish(topic, mqttMessage);
      publishSuccess = true;
      System.out.println("PUBLISH exitoso en tópico: " + topic);
    } catch (MqttException e) {
      exceptionCaught = true;
      exceptionMessage = e.getMessage();
    }

    if (expectedResult) {
      assertTrue(publishSuccess && !exceptionCaught, "PUBLISH falló en tópico: " + topic);
      System.out.println("✓ PUBLISH exitoso como se esperaba en tópico: " + topic);
    } else {
      assertTrue(exceptionCaught && !publishSuccess, "PUBLISH tuvo éxito pero se esperaba fallo en tópico: " + topic);
      System.out.println("✓ PUBLISH falló como se esperaba en tópico: " + topic);
    }
  }

  private void testSubscribe(String topic, String message, boolean expectedResult)
      throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    String[] receivedMessage = {null};

    client.setCallback(
        new MqttCallback() {
          @Override
          public void messageArrived(String topic, MqttMessage message) {
            receivedMessage[0] = new String(message.getPayload());
            latch.countDown();
          }

          @Override
          public void disconnected(MqttDisconnectResponse disconnectResponse) {}

          @Override
          public void mqttErrorOccurred(MqttException exception) {}

          @Override
          public void deliveryComplete(IMqttToken token) {}

          @Override
          public void connectComplete(boolean reconnect, String serverURI) {}

          @Override
          public void authPacketArrived(int reasonCode, MqttProperties properties) {}
        });

    try {
      client.subscribe(topic, 1);

      // Publicar mensaje de prueba usando otro cliente
      MqttClient publisherClient = new MqttClient(BROKERS[0], "TEST2", new MemoryPersistence());
      MqttConnectionOptions pubOptions = getOptions(extractBrokerId(BROKERS[0]), "stm-admin");
      publisherClient.connect(pubOptions);
      if (!topic.equals("$SYS/broker")) {
        publisherClient.publish(topic, new MqttMessage(message.getBytes()));
        publisherClient.disconnect();
      }

      boolean messageReceived = latch.await(TIMEOUT, TimeUnit.SECONDS);

      if (expectedResult) {
        assertTrue(messageReceived, "Timeout esperando mensaje en tópico: " + topic);
        assertEquals(message, receivedMessage[0]);
        System.out.println("✓ SUBSCRIBE exitoso en tópico: " + topic);
      } else {
        assertFalse(messageReceived, "Se recibió mensaje pero se esperaba timeout en tópico: " + topic);
        System.out.println("✓ SUBSCRIBE falló como se esperaba en tópico: " + topic);
      }
    } catch (MqttException e) {
      if (expectedResult) {
        throw new RuntimeException("SUBSCRIBE falló pero se esperaba éxito: " + e.getMessage());
      }
    }
  }

  private String extractBrokerId(String brokerUrl) {
    return brokerUrl.split("://")[1].split(":")[0];
  }
}
  @Test
public void stm-admin_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"stm-admin\", \"topico\": \"$SYS/broker\"}",
        true);
}

@Test
public void stm-admin_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"stm-admin\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void stm-admin_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/test\"}",
        true);
}

@Test
public void stm-admin_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void stm-admin_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/LSTM/test\"}",
        true);
}

@Test
public void stm-admin_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/LSTM/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/Conteo/test\"}",
        true);
}

@Test
public void stm-admin_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/Conteo/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/CCTV/test\"}",
        true);
}

@Test
public void stm-admin_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/CCTV/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"STMBackoffice/test\"}",
        true);
}

@Test
public void stm-admin_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"STMBackoffice/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/50/test\"}",
        true);
}

@Test
public void stm-admin_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/50/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/70/test\"}",
        true);
}

@Test
public void stm-admin_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/70/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/20/test\"}",
        true);
}

@Test
public void stm-admin_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/20/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/10/test\"}",
        true);
}

@Test
public void stm-admin_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/10/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/13/test\"}",
        true);
}

@Test
public void stm-admin_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/13/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/18/test\"}",
        true);
}

@Test
public void stm-admin_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/18/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/39/test\"}",
        true);
}

@Test
public void stm-admin_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/39/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/29/test\"}",
        true);
}

@Test
public void stm-admin_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/29/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/35/test\"}",
        true);
}

@Test
public void stm-admin_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/35/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/33/test\"}",
        true);
}

@Test
public void stm-admin_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/33/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/41/test\"}",
        true);
}

@Test
public void stm-admin_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/41/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/32/test\"}",
        true);
}

@Test
public void stm-admin_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"Operador/32/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        true);
}

@Test
public void stm-admin_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        true);
}

@Test
public void stm-admin_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        true);
}

@Test
public void stm-admin_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        true);
}

@Test
public void stm-admin_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        true);
}

@Test
public void stm-admin_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        true);
}

@Test
public void stm-admin_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        true);
}

@Test
public void stm-admin_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        true);
}

@Test
public void stm-admin_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void stm-admin_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void stm-admin_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"STMRespuestaComando/test\"}",
        true);
}

@Test
public void stm-admin_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm-admin",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"stm-admin\", \"topico\": \"STMRespuestaComando/test\"}",
        true);
}

@Test
public void STM_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"STM\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void STM_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"STM\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void STM_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/test\"}",
        true);
}

@Test
public void STM_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/test\"}",
        true);
}

@Test
public void STM_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"STM\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void STM_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"STM\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void STM_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/LSTM/test\"}",
        true);
}

@Test
public void STM_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/LSTM/test\"}",
        true);
}

@Test
public void STM_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void STM_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void STM_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void STM_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void STM_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"STM\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void STM_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"STM\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void STM_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void STM_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void STM_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void STM_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void STM_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void STM_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void STM_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void STM_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void STM_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void STM_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void STM_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void STM_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void STM_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void STM_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void STM_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void STM_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void STM_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void STM_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void STM_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void STM_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void STM_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void STM_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void STM_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void STM_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"STM\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void STM_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        true);
}

@Test
public void STM_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void STM_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void STM_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void STM_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void STM_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void STM_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void STM_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void STM_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void STM_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"STM\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void STM_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"STM\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void STM_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "STM",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"STM\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void stm_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"stm\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void stm_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"stm\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void stm_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/test\"}",
        true);
}

@Test
public void stm_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/test\"}",
        true);
}

@Test
public void stm_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"stm\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void stm_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"stm\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void stm_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/LSTM/test\"}",
        true);
}

@Test
public void stm_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/LSTM/test\"}",
        true);
}

@Test
public void stm_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void stm_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void stm_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void stm_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void stm_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"stm\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void stm_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"stm\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void stm_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void stm_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void stm_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void stm_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void stm_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void stm_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void stm_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void stm_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void stm_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void stm_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void stm_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void stm_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void stm_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void stm_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void stm_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void stm_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void stm_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void stm_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void stm_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void stm_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void stm_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void stm_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void stm_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void stm_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"stm\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void stm_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        true);
}

@Test
public void stm_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void stm_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void stm_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void stm_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void stm_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void stm_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void stm_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void stm_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void stm_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"stm\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void stm_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"stm\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void stm_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "stm",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"stm\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void mebilor_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"mebilor\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void mebilor_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"mebilor\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void mebilor_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void mebilor_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void mebilor_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void mebilor_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void mebilor_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void mebilor_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void mebilor_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void mebilor_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void mebilor_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void mebilor_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void mebilor_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void mebilor_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void mebilor_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void mebilor_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void mebilor_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void mebilor_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void mebilor_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void mebilor_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void mebilor_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void mebilor_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void mebilor_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void mebilor_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void mebilor_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void mebilor_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void mebilor_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void mebilor_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void mebilor_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void mebilor_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void mebilor_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void mebilor_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void mebilor_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void mebilor_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void mebilor_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void mebilor_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void mebilor_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void mebilor_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void mebilor_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        true);
}

@Test
public void mebilor_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        true);
}

@Test
public void mebilor_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        true);
}

@Test
public void mebilor_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        true);
}

@Test
public void mebilor_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        true);
}

@Test
public void mebilor_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        true);
}

@Test
public void mebilor_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        true);
}

@Test
public void mebilor_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        true);
}

@Test
public void mebilor_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void mebilor_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void mebilor_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void mebilor_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "mebilor",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"mebilor\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void lstm_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"lstm\", \"topico\": \"$SYS/broker\"}",
        true);
}

@Test
public void lstm_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"lstm\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void lstm_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void lstm_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void lstm_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"lstm\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void lstm_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"lstm\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void lstm_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void lstm_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/LSTM/test\"}",
        true);
}

@Test
public void lstm_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void lstm_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void lstm_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void lstm_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void lstm_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"lstm\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void lstm_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"lstm\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void lstm_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void lstm_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void lstm_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void lstm_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void lstm_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void lstm_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void lstm_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void lstm_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void lstm_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void lstm_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void lstm_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void lstm_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void lstm_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void lstm_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void lstm_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void lstm_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void lstm_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void lstm_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void lstm_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void lstm_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void lstm_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void lstm_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void lstm_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void lstm_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"lstm\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void lstm_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void lstm_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void lstm_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void lstm_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void lstm_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void lstm_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void lstm_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void lstm_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void lstm_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void lstm_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"lstm\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void lstm_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"lstm\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void lstm_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "lstm",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"lstm\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"ciemsa\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void ciemsa_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"ciemsa\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void ciemsa_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void ciemsa_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void ciemsa_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void ciemsa_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void ciemsa_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/Conteo/test\"}",
        true);
}

@Test
public void ciemsa_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void ciemsa_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void ciemsa_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void ciemsa_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void ciemsa_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void ciemsa_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void ciemsa_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void ciemsa_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void ciemsa_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void ciemsa_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void ciemsa_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void ciemsa_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void ciemsa_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void ciemsa_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void ciemsa_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void ciemsa_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void ciemsa_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void ciemsa_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void ciemsa_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void ciemsa_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void ciemsa_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void ciemsa_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void ciemsa_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void ciemsa_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void ciemsa_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void ciemsa_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ciemsa",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"ciemsa\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"cutcsa\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void cutcsa_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"cutcsa\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void cutcsa_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void cutcsa_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void cutcsa_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void cutcsa_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void cutcsa_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void cutcsa_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void cutcsa_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void cutcsa_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/50/test\"}",
        true);
}

@Test
public void cutcsa_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void cutcsa_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void cutcsa_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void cutcsa_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void cutcsa_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void cutcsa_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void cutcsa_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void cutcsa_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void cutcsa_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void cutcsa_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void cutcsa_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void cutcsa_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void cutcsa_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void cutcsa_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void cutcsa_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void cutcsa_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void cutcsa_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void cutcsa_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void cutcsa_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void cutcsa_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void cutcsa_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void cutcsa_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void cutcsa_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cutcsa",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"cutcsa\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void coetc_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"coetc\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void coetc_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"coetc\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void coetc_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void coetc_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void coetc_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"coetc\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void coetc_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"coetc\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void coetc_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void coetc_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void coetc_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void coetc_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void coetc_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void coetc_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void coetc_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"coetc\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void coetc_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"coetc\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/10/test\"}",
        true);
}

@Test
public void coetc_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/10/test\"}",
        true);
}

@Test
public void coetc_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/13/test\"}",
        true);
}

@Test
public void coetc_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/13/test\"}",
        true);
}

@Test
public void coetc_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/18/test\"}",
        true);
}

@Test
public void coetc_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/18/test\"}",
        true);
}

@Test
public void coetc_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/39/test\"}",
        true);
}

@Test
public void coetc_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/39/test\"}",
        true);
}

@Test
public void coetc_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/29/test\"}",
        true);
}

@Test
public void coetc_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/29/test\"}",
        true);
}

@Test
public void coetc_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/35/test\"}",
        true);
}

@Test
public void coetc_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/35/test\"}",
        true);
}

@Test
public void coetc_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void coetc_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void coetc_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void coetc_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void coetc_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void coetc_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void coetc_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void coetc_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void coetc_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void coetc_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void coetc_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void coetc_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"coetc\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void coetc_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"coetc\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void coetc_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"coetc\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void coetc_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"coetc\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void coetc_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void coetc_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void coetc_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"coetc\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void coetc_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"coetc\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void coetc_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void coetc_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void coetc_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void coetc_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void coetc_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void coetc_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void coetc_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"coetc\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void coetc_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"coetc\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void coetc_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void coetc_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"coetc\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void coetc_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        true);
}

@Test
public void coetc_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        true);
}

@Test
public void coetc_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        true);
}

@Test
public void coetc_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        true);
}

@Test
public void coetc_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        true);
}

@Test
public void coetc_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        true);
}

@Test
public void coetc_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        true);
}

@Test
public void coetc_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        true);
}

@Test
public void coetc_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void coetc_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"coetc\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void coetc_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"coetc\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void coetc_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coetc",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"coetc\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void come_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"come\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void come_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"come\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void come_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void come_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void come_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"come\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void come_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"come\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void come_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void come_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void come_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void come_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void come_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void come_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void come_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"come\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void come_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"come\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void come_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void come_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void come_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/10/test\"}",
        true);
}

@Test
public void come_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/10/test\"}",
        true);
}

@Test
public void come_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/13/test\"}",
        true);
}

@Test
public void come_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/13/test\"}",
        true);
}

@Test
public void come_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/18/test\"}",
        true);
}

@Test
public void come_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/18/test\"}",
        true);
}

@Test
public void come_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/39/test\"}",
        true);
}

@Test
public void come_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/39/test\"}",
        true);
}

@Test
public void come_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/29/test\"}",
        true);
}

@Test
public void come_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/29/test\"}",
        true);
}

@Test
public void come_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/35/test\"}",
        true);
}

@Test
public void come_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/35/test\"}",
        true);
}

@Test
public void come_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void come_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void come_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void come_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void come_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void come_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void come_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void come_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void come_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void come_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void come_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void come_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void come_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void come_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void come_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"come\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void come_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"come\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void come_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"come\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void come_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"come\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void come_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void come_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void come_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"come\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void come_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"come\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void come_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void come_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void come_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void come_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void come_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void come_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void come_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"come\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void come_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"come\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void come_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void come_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void come_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void come_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void come_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void come_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void come_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void come_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void come_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void come_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void come_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void come_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void come_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"come\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void come_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        true);
}

@Test
public void come_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        true);
}

@Test
public void come_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        true);
}

@Test
public void come_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        true);
}

@Test
public void come_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        true);
}

@Test
public void come_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        true);
}

@Test
public void come_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        true);
}

@Test
public void come_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        true);
}

@Test
public void come_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void come_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"come\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void come_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"come\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void come_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "come",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"come\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void ucot_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"ucot\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void ucot_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"ucot\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void ucot_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void ucot_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void ucot_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"ucot\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void ucot_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"ucot\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void ucot_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void ucot_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void ucot_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void ucot_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void ucot_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void ucot_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void ucot_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"ucot\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void ucot_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"ucot\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/10/test\"}",
        true);
}

@Test
public void ucot_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/10/test\"}",
        true);
}

@Test
public void ucot_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/13/test\"}",
        true);
}

@Test
public void ucot_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/13/test\"}",
        true);
}

@Test
public void ucot_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/18/test\"}",
        true);
}

@Test
public void ucot_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/18/test\"}",
        true);
}

@Test
public void ucot_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/39/test\"}",
        true);
}

@Test
public void ucot_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/39/test\"}",
        true);
}

@Test
public void ucot_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/29/test\"}",
        true);
}

@Test
public void ucot_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/29/test\"}",
        true);
}

@Test
public void ucot_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/35/test\"}",
        true);
}

@Test
public void ucot_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/35/test\"}",
        true);
}

@Test
public void ucot_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void ucot_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void ucot_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void ucot_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void ucot_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void ucot_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void ucot_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void ucot_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void ucot_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void ucot_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void ucot_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void ucot_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"ucot\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void ucot_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"ucot\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void ucot_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"ucot\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void ucot_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"ucot\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void ucot_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void ucot_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void ucot_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"ucot\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void ucot_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"ucot\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void ucot_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void ucot_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void ucot_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void ucot_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void ucot_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void ucot_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void ucot_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"ucot\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void ucot_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"ucot\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void ucot_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void ucot_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"ucot\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void ucot_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        true);
}

@Test
public void ucot_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        true);
}

@Test
public void ucot_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        true);
}

@Test
public void ucot_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        true);
}

@Test
public void ucot_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        true);
}

@Test
public void ucot_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        true);
}

@Test
public void ucot_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        true);
}

@Test
public void ucot_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        true);
}

@Test
public void ucot_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void ucot_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"ucot\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void ucot_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"ucot\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void ucot_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "ucot",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"ucot\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void casanova_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"casanova\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void casanova_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"casanova\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void casanova_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void casanova_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void casanova_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"casanova\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void casanova_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"casanova\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void casanova_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void casanova_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void casanova_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void casanova_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void casanova_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void casanova_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void casanova_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"casanova\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void casanova_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"casanova\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void casanova_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void casanova_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void casanova_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void casanova_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void casanova_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void casanova_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void casanova_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/10/test\"}",
        true);
}

@Test
public void casanova_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/10/test\"}",
        true);
}

@Test
public void casanova_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/13/test\"}",
        true);
}

@Test
public void casanova_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/13/test\"}",
        true);
}

@Test
public void casanova_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/18/test\"}",
        true);
}

@Test
public void casanova_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/18/test\"}",
        true);
}

@Test
public void casanova_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/39/test\"}",
        true);
}

@Test
public void casanova_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/39/test\"}",
        true);
}

@Test
public void casanova_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/29/test\"}",
        true);
}

@Test
public void casanova_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/29/test\"}",
        true);
}

@Test
public void casanova_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/35/test\"}",
        true);
}

@Test
public void casanova_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/35/test\"}",
        true);
}

@Test
public void casanova_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void casanova_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void casanova_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void casanova_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void casanova_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void casanova_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"casanova\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void casanova_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void casanova_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void casanova_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void casanova_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void casanova_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void casanova_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void casanova_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void casanova_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void casanova_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void casanova_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"casanova\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void casanova_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"casanova\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void casanova_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "casanova",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"casanova\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void copsa_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"copsa\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void copsa_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"copsa\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void copsa_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void copsa_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void copsa_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"copsa\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void copsa_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"copsa\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void copsa_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void copsa_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void copsa_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void copsa_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void copsa_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void copsa_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void copsa_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"copsa\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void copsa_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"copsa\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void copsa_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void copsa_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void copsa_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void copsa_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void copsa_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void copsa_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void copsa_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/10/test\"}",
        true);
}

@Test
public void copsa_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/10/test\"}",
        true);
}

@Test
public void copsa_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/13/test\"}",
        true);
}

@Test
public void copsa_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/13/test\"}",
        true);
}

@Test
public void copsa_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/18/test\"}",
        true);
}

@Test
public void copsa_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/18/test\"}",
        true);
}

@Test
public void copsa_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/39/test\"}",
        true);
}

@Test
public void copsa_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/39/test\"}",
        true);
}

@Test
public void copsa_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/29/test\"}",
        true);
}

@Test
public void copsa_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/29/test\"}",
        true);
}

@Test
public void copsa_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/35/test\"}",
        true);
}

@Test
public void copsa_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/35/test\"}",
        true);
}

@Test
public void copsa_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void copsa_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void copsa_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void copsa_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void copsa_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void copsa_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"copsa\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void copsa_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void copsa_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void copsa_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void copsa_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void copsa_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void copsa_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void copsa_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void copsa_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void copsa_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void copsa_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"copsa\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        true);
}

@Test
public void copsa_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"copsa\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void copsa_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "copsa",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"copsa\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void zeballos_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"zeballos\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void zeballos_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"zeballos\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void zeballos_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void zeballos_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void zeballos_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void zeballos_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void zeballos_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void zeballos_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void zeballos_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void zeballos_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void zeballos_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void zeballos_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void zeballos_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void zeballos_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void zeballos_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void zeballos_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void zeballos_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void zeballos_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void zeballos_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void zeballos_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void zeballos_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void zeballos_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void zeballos_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void zeballos_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void zeballos_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void zeballos_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void zeballos_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void zeballos_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void zeballos_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void zeballos_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void zeballos_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void zeballos_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void zeballos_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void zeballos_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/33/test\"}",
        true);
}

@Test
public void zeballos_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void zeballos_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void zeballos_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void zeballos_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void zeballos_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void zeballos_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void zeballos_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void zeballos_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void zeballos_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void zeballos_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void zeballos_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void zeballos_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void zeballos_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void zeballos_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void zeballos_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void zeballos_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "zeballos",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"zeballos\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void cita_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"cita\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void cita_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"cita\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void cita_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void cita_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void cita_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"cita\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void cita_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"cita\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void cita_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void cita_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void cita_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void cita_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void cita_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void cita_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void cita_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"cita\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void cita_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"cita\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void cita_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void cita_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void cita_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void cita_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void cita_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void cita_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void cita_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void cita_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void cita_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void cita_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void cita_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void cita_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void cita_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void cita_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void cita_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void cita_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void cita_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void cita_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void cita_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void cita_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void cita_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void cita_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void cita_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void cita_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"cita\", \"topico\": \"Operador/32/test\"}",
        true);
}

@Test
public void cita_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void cita_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void cita_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void cita_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void cita_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void cita_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void cita_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void cita_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void cita_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void cita_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"cita\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void cita_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"cita\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void cita_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "cita",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"cita\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"TalaPando\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void TalaPando_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"TalaPando\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void TalaPando_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void TalaPando_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void TalaPando_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void TalaPando_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void TalaPando_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void TalaPando_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void TalaPando_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void TalaPando_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void TalaPando_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void TalaPando_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void TalaPando_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void TalaPando_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void TalaPando_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void TalaPando_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void TalaPando_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void TalaPando_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void TalaPando_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void TalaPando_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/41/test\"}",
        true);
}

@Test
public void TalaPando_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void TalaPando_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void TalaPando_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void TalaPando_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void TalaPando_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void TalaPando_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void TalaPando_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void TalaPando_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void TalaPando_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void TalaPando_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void TalaPando_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void TalaPando_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void TalaPando_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "TalaPando",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"TalaPando\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"coDelEste\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void coDelEste_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"coDelEste\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void coDelEste_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void coDelEste_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void coDelEste_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void coDelEste_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void coDelEste_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void coDelEste_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void coDelEste_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void coDelEste_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void coDelEste_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void coDelEste_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/20/test\"}",
        true);
}

@Test
public void coDelEste_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void coDelEste_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void coDelEste_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void coDelEste_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void coDelEste_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void coDelEste_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void coDelEste_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void coDelEste_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void coDelEste_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void coDelEste_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void coDelEste_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void coDelEste_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void coDelEste_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void coDelEste_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void coDelEste_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void coDelEste_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void coDelEste_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void coDelEste_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void coDelEste_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void coDelEste_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void coDelEste_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "coDelEste",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"coDelEste\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void rutasDelNorte_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void rutasDelNorte_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/70/test\"}",
        true);
}

@Test
public void rutasDelNorte_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void rutasDelNorte_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void rutasDelNorte_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void rutasDelNorte_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void rutasDelNorte_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void rutasDelNorte_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void rutasDelNorte_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "rutasDelNorte",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"rutasDelNorte\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void satt_subscribe_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "$SYS/broker",
        "{\"usuario\": \"satt\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void satt_publish_$SYS_broker() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "$SYS/broker",
        "{\"usuario\": \"satt\", \"topico\": \"$SYS/broker\"}",
        false);
}

@Test
public void satt_subscribe_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "InfoLocal/test",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void satt_publish_InfoLocal_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "InfoLocal/test",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/test\"}",
        false);
}

@Test
public void satt_subscribe_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "STMBuses/test",
        "{\"usuario\": \"satt\", \"topico\": \"STMBuses/test\"}",
        true);
}

@Test
public void satt_publish_STMBuses_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "STMBuses/test",
        "{\"usuario\": \"satt\", \"topico\": \"STMBuses/test\"}",
        false);
}

@Test
public void satt_subscribe_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void satt_publish_InfoLocal_LSTM_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "InfoLocal/LSTM/test",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/LSTM/test\"}",
        false);
}

@Test
public void satt_subscribe_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void satt_publish_InfoLocal_Conteo_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "InfoLocal/Conteo/test",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/Conteo/test\"}",
        false);
}

@Test
public void satt_subscribe_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void satt_publish_InfoLocal_CCTV_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "InfoLocal/CCTV/test",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/CCTV/test\"}",
        false);
}

@Test
public void satt_subscribe_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "STMBackoffice/test",
        "{\"usuario\": \"satt\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void satt_publish_STMBackoffice_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "STMBackoffice/test",
        "{\"usuario\": \"satt\", \"topico\": \"STMBackoffice/test\"}",
        false);
}

@Test
public void satt_subscribe_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "Operador/50/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/50/test\"}",
        false);
}

@Test
public void satt_publish_Operador_50_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "Operador/50/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/50/test\"}",
        true);
}

@Test
public void satt_subscribe_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "Operador/70/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void satt_publish_Operador_70_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "Operador/70/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/70/test\"}",
        false);
}

@Test
public void satt_subscribe_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "Operador/20/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void satt_publish_Operador_20_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "Operador/20/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/20/test\"}",
        false);
}

@Test
public void satt_subscribe_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "Operador/10/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void satt_publish_Operador_10_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "Operador/10/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/10/test\"}",
        false);
}

@Test
public void satt_subscribe_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "Operador/13/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void satt_publish_Operador_13_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "Operador/13/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/13/test\"}",
        false);
}

@Test
public void satt_subscribe_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "Operador/18/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void satt_publish_Operador_18_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "Operador/18/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/18/test\"}",
        false);
}

@Test
public void satt_subscribe_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "Operador/39/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void satt_publish_Operador_39_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "Operador/39/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/39/test\"}",
        false);
}

@Test
public void satt_subscribe_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "Operador/29/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void satt_publish_Operador_29_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "Operador/29/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/29/test\"}",
        false);
}

@Test
public void satt_subscribe_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "Operador/35/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void satt_publish_Operador_35_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "Operador/35/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/35/test\"}",
        false);
}

@Test
public void satt_subscribe_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "Operador/33/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void satt_publish_Operador_33_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "Operador/33/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/33/test\"}",
        false);
}

@Test
public void satt_subscribe_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "Operador/41/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void satt_publish_Operador_41_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "Operador/41/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/41/test\"}",
        false);
}

@Test
public void satt_subscribe_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "Operador/32/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void satt_publish_Operador_32_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "Operador/32/test",
        "{\"usuario\": \"satt\", \"topico\": \"Operador/32/test\"}",
        false);
}

@Test
public void satt_subscribe_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void satt_publish_InfoLocal_DatosVehiculo25() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "InfoLocal/DatosVehiculo25",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/DatosVehiculo25\"}",
        false);
}

@Test
public void satt_subscribe_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void satt_publish_InfoLocal_EstadoOperativo25() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "InfoLocal/EstadoOperativo25",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/EstadoOperativo25\"}",
        false);
}

@Test
public void satt_subscribe_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void satt_publish_InfoLocal_EstadoServicio25() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "InfoLocal/EstadoServicio25",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/EstadoServicio25\"}",
        false);
}

@Test
public void satt_subscribe_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void satt_publish_InfoLocal_ParadaActual25() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "InfoLocal/ParadaActual25",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/ParadaActual25\"}",
        false);
}

@Test
public void satt_subscribe_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void satt_publish_InfoLocal_Imagenes_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "InfoLocal/Imagenes/test",
        "{\"usuario\": \"satt\", \"topico\": \"InfoLocal/Imagenes/test\"}",
        false);
}

@Test
public void satt_subscribe_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "subscribe",
        "STMRespuestaComando/test",
        "{\"usuario\": \"satt\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

@Test
public void satt_publish_STMRespuestaComando_test() throws Exception {
    testOperation(
        BROKERS[0],
        "satt",
        "publish",
        "STMRespuestaComando/test",
        "{\"usuario\": \"satt\", \"topico\": \"STMRespuestaComando/test\"}",
        false);
}

// Total de casos generados: 1050

  /////////////////////////////// FIN TEST DEL BUS

}

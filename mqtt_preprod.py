import json
import paho.mqtt.client as mqtt

# Callback cuando se conecta al broker
def on_connect(client, userdata, flags, rc):
    print(f"Conectado con resultado: {rc}")
    # Suscribirse a todos los tópicos
    client.subscribe("STMBuses/50/857/#", qos=2)

# Callback cuando se recibe un mensaje
def on_message(client, userdata, message):
    # Decodificar el mensaje recibido
    try:
        mensaje_recibido = message.payload.decode()
        json_recibido = json.loads(mensaje_recibido)
        print("\nNuevo mensaje recibido:")
        print(f"Tópico: {message.topic}")
        print(json.dumps(json_recibido, indent=4))
    except json.JSONDecodeError:
        print("Error al decodificar el mensaje JSON")
    except Exception as e:
        print(f"Error: {e}")

# Crear un cliente MQTT
client = mqtt.Client()

# Asignar los callbacks
client.on_connect = on_connect
client.on_message = on_message

# Configurar autenticación
# client.username_pw_set(username="STM-admin", password="1Dw4KjIM1lQ")

# Conectar al broker preproducción
client.connect("broker-stm-preprod.apps.ocp4-prod.imm.gub.uy", 1883, 60)

# Iniciar el bucle de red
client.loop_start()

try:
    while True:
        pass  # Mantener el script en ejecución
except KeyboardInterrupt:
    print("\nInterrumpido por el usuario")
finally:
    client.loop_stop()
    client.disconnect()
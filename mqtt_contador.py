import json
import paho.mqtt.client as mqtt

# Almacenar los identificadores únicos de los brokers locales (Bus Número)
buses_numeros = set()

# Callback cuando se conecta al broker
def on_connect(client, userdata, flags, rc):
    print(f"Conectado con resultado: {rc}")
    client.subscribe("STMBuses/50/#", qos=2)

# Callback cuando se recibe un mensaje
def on_message(client, userdata, message):
    global buses_numeros

    try:
        # Decodificar el mensaje recibido
        mensaje_recibido = message.payload.decode()
        json_recibido = json.loads(mensaje_recibido)

        # Extraer el Bus Número (tercera parte del tópico)
        bus_numero = message.topic.split("/")[2]  # Extraer la parte "125"
        buses_numeros.add(bus_numero)  # Agregar al conjunto

        print("\nNuevo mensaje recibido:")
        print(f"Tópico: {message.topic}")
        print(json.dumps(json_recibido, indent=4))

        # Mostrar la cantidad de buses únicos
        print(f"\nCantidad de buses únicos que enviaron mensajes: {len(buses_numeros)}")

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
#client.username_pw_set(username="stm-admin", password="1Dw4KjIM1lQ")
client.username_pw_set(username="stm", password="Y4208BOly7ZM")

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

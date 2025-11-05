import unittest
import json
from todos_valida_estructura import validar_mensaje, EstructuraEsperada  # Importar la función y la estructura esperada a probar

class TestMQTTValidation(unittest.TestCase):
    """Conjunto de tests para validar la estructura de mensajes MQTT."""

    def test_validar_mensaje_exito(self):
        """Mensaje con todos los campos y tipos correctos -> debe pasar la validación."""
        mensaje = {
            "Version": 301,
            "MDTEntidad": "2023-08-09T21:15:00-03:00",
            "Latitud": -34.89446,
            "Longitud": -56.1528,
            "Odometro": 50000,
            "EmpresaCod": 50,
            "BusNro": 857,
            "EstadoServicioCodigo": 1,
            "NumeroServicio": 1234,
            "TipoMinuta": 3,
            "TipoInicio": 1,
            "IdInternoOperador": 1234
        }
        resultado = validar_mensaje(mensaje, EstructuraEsperada["EstadoServicio"])
        self.assertTrue(resultado)

    def test_validar_mensaje_tipo_incorrecto(self):
        """Tipo de campo incorrecto (Version como string) -> debe fallar la validación."""
        mensaje = {
            "Version": "301",  # Tipo incorrecto (debe ser int)
            "MDTEntidad": "2023-08-09T21:15:00-03:00",
            "Latitud": -34.89446,
            "Longitud": -56.1528,
            "Odometro": 50000,
            "EmpresaCod": 50,
            "BusNro": 857,
            "EstadoServicioCodigo": 1,
            "NumeroServicio": 1234,
            "TipoMinuta": 3,
            "TipoInicio": 1,
            "IdInternoOperador": 1234
        }
        resultado = validar_mensaje(mensaje, EstructuraEsperada["EstadoServicio"])
        self.assertFalse(resultado)

    def test_validar_mensaje_empresa_bus(self):
        """EmpresaCod y BusNro diferentes de los esperados -> debe fallar la validación."""
        mensaje = {
            "Version": 301,
            "MDTEntidad": "2023-08-09T21:15:00-03:00",
            "Latitud": -34.89446,
            "Longitud": -56.1528,
            "Odometro": 50000,
            "EmpresaCod": 51,  # Incorrecto
            "BusNro": 858,     # Incorrecto
            "EstadoServicioCodigo": 1,
            "NumeroServicio": 1234,
            "TipoMinuta": 3,
            "TipoInicio": 1,
            "IdInternoOperador": 1234
        }
        resultado = validar_mensaje(mensaje, EstructuraEsperada["EstadoServicio"])
        self.assertFalse(resultado)

    def test_validar_mensaje_empresa_cod_incorrecto(self):
        """Solo EmpresaCod incorrecto -> debe fallar."""
        mensaje = {
            "Version": 301,
            "MDTEntidad": "2023-08-09T21:15:00-03:00",
            "Latitud": -34.89446,
            "Longitud": -56.1528,
            "Odometro": 50000,
            "EmpresaCod": 51,  # Incorrecto
            "BusNro": 857,
            "EstadoServicioCodigo": 1,
            "NumeroServicio": 1234,
            "TipoMinuta": 3,
            "TipoInicio": 1,
            "IdInternoOperador": 1234
        }
        resultado = validar_mensaje(mensaje, EstructuraEsperada["EstadoServicio"])
        self.assertFalse(resultado)

    def test_validar_mensaje_bus_nro_incorrecto(self):
        """Solo BusNro incorrecto -> debe fallar."""
        mensaje = {
            "Version": 301,
            "MDTEntidad": "2023-08-09T21:15:00-03:00",
            "Latitud": -34.89446,
            "Longitud": -56.1528,
            "Odometro": 50000,
            "EmpresaCod": 50,
            "BusNro": 858,  # Incorrecto
            "EstadoServicioCodigo": 1,
            "NumeroServicio": 1234,
            "TipoMinuta": 3,
            "TipoInicio": 1,
            "IdInternoOperador": 1234
        }
        resultado = validar_mensaje(mensaje, EstructuraEsperada["EstadoServicio"])
        self.assertFalse(resultado)

    def test_mensaje_no_json(self):
        """Comprobar que intentar parsear un string no-JSON lanza JSONDecodeError."""
        mensaje = "Esto no es un JSON"
        with self.assertRaises(json.JSONDecodeError):
            json.loads(mensaje)

    def test_validar_mensaje_faltante_campo(self):
        """Mensaje faltando un campo obligatorio (IdInternoOperador) -> debe fallar."""
        mensaje = {
            "Version": 301,
            "MDTEntidad": "2023-08-09T21:15:00-03:00",
            "Latitud": -34.89446,
            "Longitud": -56.1528,
            "Odometro": 50000,
            "EmpresaCod": 50,
            "BusNro": 857,
            "EstadoServicioCodigo": 1,
            "NumeroServicio": 1234,
            "TipoMinuta": 3,
            "TipoInicio": 1
            # Falta el campo "IdInternoOperador"
        }
        resultado = validar_mensaje(mensaje, EstructuraEsperada["EstadoServicio"])
        self.assertFalse(resultado)

if __name__ == '__main__':
    # Ejecuta los tests cuando se ejecute este archivo directamente.
    unittest.main()

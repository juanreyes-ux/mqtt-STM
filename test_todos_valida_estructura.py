import unittest
import json
from todos_valida_estructura import validar_mensaje, EstructuraEsperada  # Asegúrate de importar los elementos correctos

class TestMQTTValidation(unittest.TestCase):

    def test_validar_mensaje_exito(self):
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
    mensaje = "Esto no es un JSON"
    with self.assertRaises(json.JSONDecodeError):
        json.loads(mensaje)

def test_validar_mensaje_faltante_campo(self):
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
    unittest.main()

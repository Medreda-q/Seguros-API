# Registro de Errores y Bugs (BUGS.md)

Este documento registra de forma cronológica cada error o comportamiento anómalo detectado durante el desarrollo y las pruebas (ambientes dev y test), identificando el síntoma, la causa raíz y la solución implementada.

| Fecha (YYYY-MM-DD) | Entorno (dev / test) | Síntoma observado | Causa raíz | Solución aplicada |
| :--- | :--- | :--- | :--- | :--- |
| *Pendiente* | *dev* | *Ejemplo: 400 Bad Request en DNI válido* | *Expresión regular no contempla letra mayúscula* | *Normalizar cadena con `.toUpperCase()` antes de validar* |


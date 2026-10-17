# Registro de Errores y Bugs (BUGS.md)

Este documento registra de forma cronológica cada error o comportamiento anómalo detectado durante el desarrollo y las pruebas (ambientes dev y test), identificando el síntoma, la causa raíz y la solución implementada.

| Fecha (YYYY-MM-DD) | Entorno (dev / test) | Síntoma observado | Causa raíz | Solución aplicada |
| :--- | :--- | :--- | :--- | :--- |
| 2026-10-17 | test | Fallo en test JUnit `shouldValidateValidNie` para `Y1234567S` y `Z1234567B` | Error de cálculo aritmético manual en los datos de prueba: $11234567 \pmod{23} = 10 \rightarrow \text{'X'}$ y $21234567 \pmod{23} = 1 \rightarrow \text{'R'}$ | Corregir los casos de prueba con los valores reales calculados por el algoritmo: `Y1234567X` y `Z1234567R`. |

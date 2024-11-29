#!/bin/bash

# Definir cuentas y correos
declare -A USERS
USERS["Cesars2130"]="Cesars.213008@gmail.com"
USERS["catru2"]="camachotrujillomartinx@gmail.com"
USERS["DiegoG0477"]="diego.14.13.lopez@gmail.com"
USERS["0511Cynthia"]="223205@ids.upchiapas.edu.mx"

# Definir hosts personalizados (configurados en ~/.ssh/config)
declare -A HOSTS
HOSTS["Cesars2130"]="github-cesar"
HOSTS["catru2"]="github-martin"
HOSTS["DiegoG0477"]="github-diego"
HOSTS["0511Cynthia"]="github-cynthia"

# Mostrar opciones con índices numéricos
echo "Selecciona la cuenta de Git/GitHub a usar:"
keys=("${!USERS[@]}") # Guardar las claves en un array
for i in "${!keys[@]}"; do
    key="${keys[$i]}"
    echo "  $i) $key (${USERS[$key]})"
done

# Leer selección numérica
read -p "Ingresa el número de la cuenta: " seleccion

# Validar selección
if [[ -z "${keys[$seleccion]}" ]]; then
    echo "Error: selección no válida."
    exit 1
fi

cuenta="${keys[$seleccion]}"

# Configurar usuario y correo para Git
git config --global user.name "$cuenta"
git config --global user.email "${USERS[$cuenta]}"
echo "Configuración de Git actualizada: ${USERS[$cuenta]}"

# Cambiar clave SSH si aplica
if [[ -n "${HOSTS[$cuenta]}" ]]; then
    echo "Configurando conexión SSH para '${HOSTS[$cuenta]}'..."
    git remote set-url origin "git@${HOSTS[$cuenta]}:0511Cynthia/restaurante.git"
    echo "URL del remoto actualizada para usar '${HOSTS[$cuenta]}'."
fi

# Verificar configuración actual
echo "Usuario actual:"
git config user.name
git config user.email
echo "URL remota actual:"
git remote -v

# Probar conexión SSH
echo "Probar conexión SSH..."
ssh -T "git@${HOSTS[$cuenta]}" || {
    echo "Error: Fallo en la conexión SSH con '${HOSTS[$cuenta]}'. Verifica tu configuración."
    exit 1
}


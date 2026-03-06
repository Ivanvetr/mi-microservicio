# Microservicio K8S — Java 25 + Spring Boot 4.0.3

Microservicio REST desplegado con un stack completo de tecnologías modernas de contenedores y orquestación.

---

## Stack Tecnológico

| Tecnología | Versión | Rol |
|---|---|---|
| Java | 25 | Lenguaje del microservicio |
| Spring Boot | 4.0.3 | Framework REST |
| Docker | 4.x | Contenerización |
| Kubernetes (k3d) | v1.31.5 | Orquestación de contenedores |
| Helm | v3.14+ | Gestión de paquetes K8S |
| ArgoCD | v3.3.2 | GitOps / Despliegue continuo |
| GitHub Actions | — | Pipeline CI/CD |
| Docker Hub | — | Registro de imágenes |

---

## Estructura del Proyecto

```
mi-microservicio/
├── src/
│   └── main/
│       ├── java/com/miempresa/mimicroservicio/
│       │   ├── MiMicroservicioApplication.java   # Clase principal
│       │   └── controller/
│       │       └── ApiController.java             # Endpoints REST
│       └── resources/
│           └── application.yml                    # Configuración
├── Dockerfile                                     # Imagen multi-stage
├── .dockerignore
├── pom.xml                                        # Dependencias Maven
├── helm/
│   └── mi-servicio/
│       ├── Chart.yaml                             # Metadata del chart
│       ├── values.yaml                            # Valores por defecto
│       └── templates/
│           ├── deployment.yaml                    # Deployment K8S
│           ├── service.yaml                       # Service K8S
│           └── ingress.yaml                       # Ingress K8S
├── argocd/
│   └── application.yaml                           # Definición ArgoCD
└── .github/
    └── workflows/
        └── ci-cd.yml                              # Pipeline CI/CD
```

---

## Endpoints del Microservicio

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/` | Respuesta principal con versión y entorno |
| GET | `/info` | Información del servicio y endpoints disponibles |
| GET | `/actuator/health` | Health check para Kubernetes probes |

### Ejemplo de respuesta — `GET /`

```json
{
  "mensaje": "Microservicio Java 25 funcionando en Kubernetes!",
  "version": "1.0.0",
  "timestamp": "2026-03-06T14:22:10.123",
  "entorno": "produccion"
}
```

### Ejemplo de respuesta — `GET /actuator/health`

```json
{
  "status": "UP"
}
```

---

## Docker

### Construcción de la imagen

```bash
docker build -t ivanvetr/mi-microservicio:1.0.0 .
```

El Dockerfile usa **multi-stage build**:
- **Etapa 1:** Compila el proyecto con Maven y JDK 25
- **Etapa 2:** Imagen final liviana con solo el JRE 25 y el `.jar`

### Ejecutar localmente

```bash
docker run -d -p 8080:8080 --name test-ms ivanvetr/mi-microservicio:1.0.0
curl http://localhost:8080/
docker stop test-ms && docker rm test-ms
```

### Imagen en Docker Hub

```
ivanvetr/mi-microservicio:latest
ivanvetr/mi-microservicio:1.0.0
```

---

## Kubernetes

### Requisitos previos

- Docker Desktop corriendo
- k3d instalado

### Crear el clúster

```bash
k3d cluster create mi-cluster --agents 1 --port "8080:80@loadbalancer" --port "8443:443@loadbalancer"
kubectl create namespace mi-app
kubectl create namespace argocd
```

### Verificar el clúster

```bash
kubectl get nodes
kubectl get namespaces
```

---

## Helm

### Estructura del chart

El chart de Helm gestiona tres recursos de Kubernetes:
- **Deployment** — define los pods y sus configuraciones
- **Service** — expone el deployment internamente en el clúster
- **Ingress** — enruta el tráfico externo al servicio

### Desplegar con Helm

```bash
# Validar el chart
helm lint helm/mi-servicio/

# Instalar
helm install mi-microservicio helm/mi-servicio/ --namespace mi-app

# Actualizar
helm upgrade mi-microservicio helm/mi-servicio/ --namespace mi-app

# Ver releases instalados
helm list -n mi-app
```

### Personalizar por entorno

```bash
# Desplegar con valores de staging
helm upgrade mi-microservicio helm/mi-servicio/ \
  --namespace mi-app \
  -f helm/mi-servicio/values-staging.yaml
```

---

## ArgoCD — GitOps

ArgoCD implementa el patrón **GitOps**: mantiene el clúster de Kubernetes sincronizado con el estado definido en este repositorio Git.

### Instalar ArgoCD

```bash
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl wait --for=condition=Ready pods --all -n argocd --timeout=300s
```

### Acceder al UI

```bash
# Abrir acceso al UI
kubectl port-forward svc/argocd-server -n argocd 9090:443

# Obtener contraseña del admin (Linux/Mac)
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d

# Obtener contraseña del admin (Windows PowerShell)
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | ForEach-Object { [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($_)) }
```

Accede en: **https://localhost:9090** — Usuario: `admin`

### Desplegar la Application

```bash
kubectl apply -f argocd/application.yaml
```

ArgoCD detectará automáticamente cualquier cambio en `helm/mi-servicio/` y lo desplegará en el clúster sin intervención manual.

---

## Pipeline CI/CD — GitHub Actions

El pipeline se activa automáticamente con cada `git push` a la rama `main` y ejecuta 3 jobs en secuencia:

```
git push → [Build & Test] → [Build & Push Docker] → [Update Helm Tag] → ArgoCD despliega
```

### Jobs del pipeline

| Job | Descripción |
|---|---|
| `build-test` | Compila el proyecto y ejecuta los tests con Maven |
| `build-push` | Construye la imagen Docker y la sube a Docker Hub con el SHA del commit |
| `update-helm` | Actualiza el `image.tag` en `values.yaml` para que ArgoCD detecte el cambio |

### Secrets requeridos en GitHub

Ve a **Settings → Secrets and variables → Actions** y agrega:

| Secret | Descripción |
|---|---|
| `DOCKERHUB_USERNAME` | Tu username de Docker Hub |
| `DOCKERHUB_TOKEN` | Token de acceso de Docker Hub (Read, Write, Delete) |

### Flujo completo automatizado

1. Desarrollador hace `git push` a `main`
2. GitHub Actions compila y testea el código
3. Se construye y sube la nueva imagen a Docker Hub con tag `<sha>`
4. El pipeline actualiza `helm/mi-servicio/values.yaml` con el nuevo tag
5. ArgoCD detecta el cambio en Git (~3 minutos)
6. ArgoCD despliega automáticamente la nueva versión en Kubernetes

---

## Verificación del sistema

```bash
# Estado de los pods
kubectl get pods -n mi-app
kubectl get pods -n argocd

# Servicios
kubectl get services -n mi-app

# Helm
helm list -n mi-app

# Probar endpoints
kubectl port-forward svc/mi-microservicio-service 8888:80 -n mi-app
curl http://localhost:8888/
curl http://localhost:8888/info
curl http://localhost:8888/actuator/health
```

---

## Troubleshooting

| Problema | Comando para diagnosticar |
|---|---|
| Pod no arranca | `kubectl logs NOMBRE-POD -n mi-app` |
| Pod en CrashLoopBackOff | `kubectl logs NOMBRE-POD -n mi-app --previous` |
| Pod en Pending | `kubectl describe pod NOMBRE-POD -n mi-app` |
| ArgoCD no sincroniza | Verificar en https://localhost:9090 y hacer Sync manual |
| Pipeline falla en Maven | `git update-index --chmod=+x mvnw` |
| Push rechazado | `git pull origin main` antes de `git push` |

---

## Autor

**Ivan Vera** — [@ivanvetr](https://github.com/ivanvetr)

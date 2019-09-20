#!/usr/bin/env bash



function installKafka() {
    if [[ ! -d "kubernetes-kafka" ]]; then
      echo "cloning kubernetes-kafka"
      git clone https://github.com/aaronp/kubernetes-kafka.git
    else
      echo "kubernetes-kafka already downloaded"
    fi;

    pushd kubernetes-kafka

    echo "applying namespace"
    kubectl apply -f 00-namespace.yml
    echo "applying rbac"
    kubectl apply -R -f rbac-namespace-default
    echo "applying zk"
    kubectl apply -R -f zookeeper
    echo "applying kafka"
    kubectl apply -f kafka

    popd

    echo "kubectl get Pods --namespace=kafka"
    kubectl get Pods --namespace=kafka
}
# https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/
function configureKafka4m() {
  echo "creating kafka4m-config config"
  kubectl create configmap kafka4m-config --from-file=config/kafka4m-k8s.conf --from-file=config/logback.xml

  echo "created kafka4m-config config"
  kubectl get configmaps kafka4m-config -o yaml
}

function createSimpleHttpServer() {
  jfloff/alpine-python python3 -m http.server 8080

}
# installKafka

configureKafka4m
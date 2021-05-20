SHELL := /bin/bash
MAKEFLAGS += -rR
# -- bin and sh scripts --
build.sh := ./build.sh
kube.sh := ${bin_dir}/kube
DB_VENDOR := h2
# call it first to git clone the build/bn
shResults := $(shell $(build.sh))
# include boilerplate to set BUILD_ENV and DB from targets
include ./build/bin/Makefile-env-db.make
# calls the build.sh makeEnvFile to build the vairables file for make, recreates each make run
shResults := $(shell $(build.sh) makeEnvFile $(BUILD_ENV) $(DB_VENDOR) $(USE_BUILDER))
# import/sinclude the variables file to make it availiable to make as well
sinclude ./build/make/$(BUILD_ENV)_$(DB_VENDOR).env
# include common makefile templates
include ./build/bin/Makefile-docker-db.make
include ./build/bin/Makefile-gradle.make
include ./build/bin/Makefile.deploy-common-targets

# $(info shResults $(shResults)) # logs out the bash echo from shResults
# $(info DBMS=$(DBMS) BUILD_ENV=$(BUILD_ENV) DOCK_BUILDER_NAME=${DOCK_BUILDER_NAME} DOCK_DB_BUILD_NAME=${DOCK_DB_BUILD_NAME} DockerExec=${DockerExec} DockerDbExec=${DockerDbExec})

# --- Targets/Goals -----
# if not arguments ar provided then show help
.DEFAULT_GOAL := help

build-log-vars: start-if-builder ## uses the build.sh to log vars
	${DockerExec} ${build.sh} logVars

dockmark-serve: ## run the docs server locally
	${build.sh} dockmark-serve

gorm-tools-security-check: ## run the docs server locally
	./gradlew gorm-tools-security:check

gorm-tools-security-test: ## runs ./gradlew integrationTest
	./gradlew gorm-tools-security:test $(testArg)

test-domain-unit-test: ## runs ./gradlew integrationTest
	./gradlew app-domains:test $(testArg)

test-domain-int-test: ## runs ./gradlew integrationTest
	./gradlew app-domains:integrationTest $(testArg)

show-compile-dependencies: ## shows gorm-tools:dependencies --configuration compile
	# ./gradlew gorm-tools:dependencies --configuration compileClasspath
	./gradlew gorm-tools:dependencies --configuration compile

# -- DOCKER ---
build/docker-build/%: ##Builds docker image, for example build/docker-build/restify
	${build.sh} buildDocker $*

# run-docker-app/restify
run-docker-app/%: build/docker-build/%
	${build.sh} runDockerApp $* ${DBMS}

# dock-deploy/restify
dock-deploy/%:
	${build.sh} dockDeploy $*

# ---- Kubernetes Deploy ------
kube-clean/%: ## removes everything with the app=${APP_NAME}
# 	${kube.sh} clean app=${APP_NAME} ${KUB_NAMESPACE}
	${kube.sh} clean app="restify-v10-0-x" ${KUB_NAMESPACE}  # XXX why is APP_NAME returning just restify?

kube-deploy/%: kube-create-ns ## run deploy to rancher/kubernetes
	${build.sh} applyTpl ${DBMS} $*/src/deploy/db-deploy-${DBMS}.tpl.yml
	${build.sh} applyTpl ${DBMS} $*/src/deploy/db-service.tpl.yml
	${build.sh} applyTpl ${DBMS} $*/src/deploy/app-deploy.tpl.yml
	${build.sh} applyTpl ${DBMS} $*/src/deploy/app-service.tpl.yml
	${build.sh} applyTpl ${DBMS} $*/src/deploy/app-configmap.tpl.yml

kube-cust-deploy/%: kube-create-ns ## run deploy to rancher/kubernetes using variables set in user.env
	${build.sh} applyTpl ${DBMS} $*/src/deploy/db-deploy-${DBMS}.tpl.yml
	${build.sh} applyTpl ${DBMS} $*/src/deploy/db-service.tpl.yml
	${build.sh} applyTpl ${DBMS} $*/src/deploy/app-cust-deploy.tpl.yml
	${build.sh} applyTpl ${DBMS} $*/src/deploy/app-service.tpl.yml
	${build.sh} applyTpl ${DBMS} $*/src/deploy/app-configmap.tpl.yml

include ./build/bin/Makefile-help.make

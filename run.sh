#!/bin/bash

# .env 파일에서 환경 변수 로드 (특수 문자 처리)
set -a
source .env
set +a

# Spring Boot 실행
mvn spring-boot:run


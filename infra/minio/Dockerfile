# syntax=docker/dockerfile:1
FROM golang:1.24.2-bookworm AS build
WORKDIR /src
ADD --checksum=sha256:7eb30a913fea30f18069abf194e1e78e4983b558cc526911ae1c11396a9859a5 https://codeload.github.com/minio/minio/tar.gz/0d7408fc9969caf07de6a8c3a84f9fbb10a6739e /tmp/source.tar.gz
RUN tar -xzf /tmp/source.tar.gz --strip-components=1 -C /src
RUN --mount=type=secret,id=cloud_ca if [ -f /run/secrets/cloud_ca ]; then export SSL_CERT_FILE=/run/secrets/cloud_ca; fi; CGO_ENABLED=0 GOMAXPROCS=2 go build -p 2 -trimpath -o /minio .
FROM debian:bookworm-slim
RUN --mount=type=secret,id=cloud_ca if [ -f /run/secrets/cloud_ca ]; then cp /run/secrets/cloud_ca /usr/local/share/ca-certificates/cloud-proxy.crt; fi; apt-get update && apt-get install -y --no-install-recommends curl ca-certificates && rm -rf /var/lib/apt/lists/*
COPY --from=build /minio /usr/local/bin/minio
RUN mkdir /data && chown 10001:10001 /data
USER 10001
EXPOSE 9000 9001
ENTRYPOINT ["minio"]

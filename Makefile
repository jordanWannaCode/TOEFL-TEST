.PHONY: install up media check smoke logs down
install:
	./scripts/install.sh
up:
	./scripts/init-local.sh
	./scripts/compose.sh up -d postgres minio backend ai-service frontend --wait --wait-timeout 180
media:
	./scripts/compose.sh up --build -d minio --wait --wait-timeout 180
check:
	./scripts/check.sh
smoke:
	./scripts/smoke.sh
logs:
	./scripts/compose.sh logs --tail=100 backend ai-service frontend
down:
	./scripts/compose.sh down

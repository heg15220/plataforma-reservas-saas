FROM python:3.13.2-slim-bookworm

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /opt/reserly

COPY packages/demand-contracts ./packages/demand-contracts
COPY apps/demand-engine ./apps/demand-engine

RUN pip install --no-cache-dir \
    ./packages/demand-contracts \
    ./apps/demand-engine

USER 65532:65532

EXPOSE 8090

ENTRYPOINT ["reserly-demand-engine"]

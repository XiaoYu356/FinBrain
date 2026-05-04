FROM python:3.11-slim

WORKDIR /app

RUN adduser --disabled-password --gecos '' finbrain

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

RUN chown -R finbrain:finbrain /app

USER finbrain

EXPOSE 8000

ENV PYTHONUNBUFFERED=1

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]

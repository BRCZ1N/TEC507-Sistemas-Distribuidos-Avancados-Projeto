# 📡 Total Order Multicast com Sequencer

Este projeto implementa um sistema de **Total Order Multicast** baseado em um **sequenciador central**, garantindo que todas as mensagens sejam entregues na mesma ordem em todos os peers.

---

# 🧠 Visão Geral

O sistema é composto por:

- **Sequencer (P1)** → responsável por ordenar globalmente as mensagens
- **Peers (P2, P3, P4, ...)** → enviam e recebem mensagens

📌 Objetivo: garantir **ordem total global** das mensagens.

---

# 🏗️ Arquitetura

        P2 ----\
        P3 ----- >  Sequencer (P1) ----> Broadcast ordenado
        P4 ----/

Fluxo:
1. Peer envia mensagem
2. Sequencer recebe
3. Sequencer atribui número de sequência global (S1, S2, S3...)
4. Sequencer envia para todos os peers
5. Todos entregam na mesma ordem

---

# 🌐 Descobrir IP da máquina

## 🪟 Windows

```bash
ipconfig
```

Procure:

```
IPv4 Address. . . . . . . . . . . : 192.168.0.149
```

---

## 🐧 Linux

```bash
hostname -I
```

---

## 🍎 MacOS

```bash
ifconfig
```

ou

```bash
ipconfig getifaddr en0
```

---

📌 IMPORTANTE:
- Use o IP real da máquina (não 127.0.0.1)
- Todos os peers e sequencer devem estar na mesma rede
- Verifique firewall se não conectar

---

# ⚙️ Configuração

## 🔴 Sequencer (P1)

```properties
spring.application.name=sequencer
node.id=P1
node.host=192.168.0.149
server.port=60000
```

---

## 🔵 Peer (ex: P2)

```properties
spring.application.name=spring
node.id=P2
node.host=192.168.0.149
server.port=0

sequencer.id=P1
sequencer.host=192.168.0.149
sequencer.port=60000
```

---

# 🚀 COMANDOS DE EXECUÇÃO

## 1️⃣ Subir o Sequencer

```bash
java -jar sequencer.jar
```

📌 Deve ser iniciado primeiro.

---

## 2️⃣ Subir os Peers

Em terminais separados:

```bash
java -jar peer.jar --node.id=P2
```

```bash
java -jar peer.jar --node.id=P3
```

```bash
java -jar peer.jar --node.id=P4
```

📌 Cada peer deve ter um ID único.

---

## 💡 Exemplo completo de execução

```bash
# Terminal 1
java -jar sequencer.jar

# Terminal 2
java -jar peer.jar --node.id=P2

# Terminal 3
java -jar peer.jar --node.id=P3

# Terminal 4
java -jar peer.jar --node.id=P4
```

---

# 📤 Fluxo de mensagens

P2 envia "A"  
P3 envia "B"

Sequencer ordena:

S1 → A  
S2 → B

Todos recebem:

A → B

---

# 📌 Garantias

✔ Ordem total global  
✔ Todos veem a mesma sequência  
✔ Independência da ordem de envio  
✔ Entrega confiável via sequencer

---

# ⚠️ Problemas comuns

## ❌ Peer não conecta
- Verificar IP com `ipconfig`
- Verificar firewall
- Verificar mesma rede

---

## ❌ Sequencer não responde
- Porta 60000 livre
- IP correto configurado

---

## ❌ Ordem quebrada
- Reiniciar sequencer e peers juntos

---

# 💡 Observação importante

O sequencer é um **ponto único de falha**:
- Se cair, o sistema inteiro para
- Simplicidade em troca de centralização

---

# 🧪 Resumo final

O sistema garante ordenação total usando um sequencer central que atribui números sequenciais às mensagens e distribui para todos os peers, garantindo consistência global.
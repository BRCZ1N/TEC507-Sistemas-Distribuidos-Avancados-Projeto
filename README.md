# 📡 Total Order Multicast com ISIS

Este projeto implementa um sistema distribuído de **Total Order Multicast** utilizando o algoritmo **ISIS (Isis Total Ordering Algorithm)** e comunicação **HTTP** entre os participantes.

O objetivo é garantir que todas as mensagens sejam entregues exatamente na mesma ordem em todos os peers do sistema, independentemente da ordem em que tenham sido recebidas pela rede.

A implementação utiliza **Spring Boot** no backend e uma interface web para envio e visualização das mensagens.

---

# 🧠 Visão Geral

O sistema é composto por um conjunto de peers que participam de um grupo distribuído.

Cada peer é responsável por:

- Enviar mensagens ao grupo;
- Receber mensagens de outros peers;
- Gerar propostas de sequência;
- Participar da definição da ordem final das mensagens;
- Entregar as mensagens na mesma ordem que todos os demais peers.

Todos os peers executam exatamente o mesmo algoritmo de ordenação.

---

# 🏗️ Arquitetura

```text
                 +-------------+
                 |  Front-end  |
                 +-------------+
                  /    |    |    \
                 /     |    |     \
                /      |    |      \
       +---------+ +---------+ +---------+ +---------+
       | Peer 1  | | Peer 2  | | Peer 3  | | Peer 4  |
       +---------+ +---------+ +---------+ +---------+

          Todos executam o algoritmo ISIS
```

O Front-end é utilizado apenas para interação com o sistema, envio e visualização das mensagens.

Toda a lógica de ordenação permanece distribuída entre os peers.

---

# ⚙️ Funcionamento

Quando um peer deseja enviar uma mensagem:

1. Envia a mensagem para todos os participantes configurados.
2. Cada peer gera uma proposta de número de sequência.
3. O sender aguarda as propostas recebidas.
4. A maior proposta é escolhida como sequência final.
5. O sender envia o Agreement para todos os peers.
6. Cada peer marca a mensagem como entregável.
7. As mensagens são liberadas em ordem total.

Para evitar bloqueios indefinidos, o protocolo utiliza timeouts durante a comunicação entre os participantes. Caso uma etapa não seja concluída dentro do tempo esperado, a operação é encerrada e a mensagem pode ser descartada.

Dessa forma, todos os participantes entregam as mensagens exatamente na mesma ordem quando o protocolo é concluído com sucesso.

---

# 🌐 Descobrindo o IP da máquina

## Windows

```bash
ipconfig
```

Procure pelo endereço IPv4.

## Linux

```bash
hostname -I
```

## macOS

```bash
ifconfig
```

ou

```bash
ipconfig getifaddr en0
```

Utilize sempre o IP da rede local e nunca `127.0.0.1`.

---

# ⚙️ Configuração

Exemplo do `application.properties`:

```properties
spring.application.name=spring

logging.level.chat.sda.spring.service=INFO
logging.level.chat.sda.spring.controller=INFO
logging.level.root=ERROR

logging.pattern.console=[%level] %msg%n

node.id=${NODE_ID:1}
node.host=${NODE_HOST:192.168.1.107}
server.port=${PORT:60001}

simulation.delay.enabled=${DELAY:false}

node.peers=${NODE_PEERS:1:192.168.1.107:60001,2:192.168.1.107:60002,3:192.168.1.64:60003,4:192.168.1.107:60004}
```

Cada peer deve possuir:

- `node.id` único;
- Porta diferente;
- Endereço IP válido;
- Lista de participantes configurada em `node.peers`.

---

# 🚀 Execução

## Inicialização dos peers

### Peer 1

```bash
java -jar peer.jar --node.id=1 --server.port=60001 --node.host=192.168.1.107 --simulation.delay.enabled=true
```

### Peer 2

```bash
java -jar peer.jar --node.id=2 --server.port=60002 --node.host=192.168.1.107 --simulation.delay.enabled=false
```

### Peer 3

```bash
java -jar peer.jar --node.id=3 --server.port=60003 --node.host=192.168.1.64 --simulation.delay.enabled=false
```

### Peer 4

```bash
java -jar peer.jar --node.id=4 --server.port=60004 --node.host=192.168.1.107 --simulation.delay.enabled=false
```

## Inicialização do Front-end

```bash
npm install
npm run dev -- --host
```

Após iniciar os peers e a interface web, o sistema estará pronto para envio de mensagens.

O parâmetro `simulation.delay.enabled` pode ser utilizado para simular atrasos de rede durante os testes.

---

# ⏱️ Simulação e Tratamento de Falhas

O sistema permite simular atrasos de comunicação através da propriedade:

```properties
simulation.delay.enabled=true
```

Quando habilitada, respostas podem ser artificialmente atrasadas para validar o comportamento do protocolo sob condições de rede mais lentas.

As comunicações entre os peers utilizam timeout para evitar que mensagens permaneçam indefinidamente aguardando respostas.

Caso uma operação não consiga ser concluída dentro do tempo esperado, a comunicação será considerada falha para aquela execução do protocolo.

Mensagens que não consigam concluir todas as etapas necessárias podem ser descartadas.

---

# 📤 Exemplo de execução

Considere duas mensagens enviadas simultaneamente.

Peer 2 envia:

```text
A
```

Peer 3 envia:

```text
B
```

Cada peer gera sua proposta de sequência.

Após receber todas as propostas necessárias, o sender escolhe a maior sequência e envia o Agreement.

Todos os peers entregarão exatamente a mesma ordem.

Exemplo:

```text
A
B
```

ou

```text
B
A
```

A ordem pode variar entre execuções, mas será idêntica em todos os participantes.

---

# ✅ Garantias

O sistema fornece:

- Ordem Total (Total Order);
- Entrega consistente entre todos os peers;
- Ausência de sequenciador central;
- Ordenação distribuída baseada em propostas e acordos;
- Consistência na entrega das mensagens.

---

# ⚠️ Limitações

- O sender depende do recebimento das propostas necessárias para concluir o protocolo.
- Falhas de comunicação podem impedir a conclusão da ordenação de determinadas mensagens.
- Mensagens que excedam os limites de tempo configurados podem ser descartadas.
- O conjunto de peers é definido estaticamente através da configuração.
- O sistema foi desenvolvido com fins acadêmicos.
- Não implementa mecanismos avançados de tolerância a falhas encontrados em sistemas distribuídos de produção.

---

# 📁 Estrutura do Projeto

```text
backend/
├── controller/
├── dto/
├── model/
├── service/
└── utils/

frontend/
├── src/
├── public/
└── package.json
```

Principais componentes:

- **ChatService** → Implementação do protocolo ISIS.
- **Front-end** → Interface para envio e visualização das mensagens.

---

# 📝 Resumo

Este projeto implementa um Total Order Multicast baseado no algoritmo ISIS utilizando Spring Boot e comunicação HTTP entre os participantes. A ordenação das mensagens ocorre de forma totalmente distribuída através do mecanismo de propostas e acordos do ISIS, garantindo que todos os peers entreguem as mensagens na mesma ordem. O sistema inclui simulação de atraso de rede e mecanismos de timeout para evitar bloqueios indefinidos durante a execução do protocolo, sendo desenvolvido com foco educacional para o estudo de sistemas distribuídos.
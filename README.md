# 📡 Total Order Multicast com ISIS

Este projeto implementa um sistema distribuído de **Total Order Multicast** utilizando o algoritmo **ISIS (Isis Total Ordering Algorithm)**, garantindo que todas as mensagens sejam entregues na mesma ordem em todos os processos do grupo.

Além do algoritmo ISIS, o sistema utiliza o **algoritmo Bully** para eleição de líder, responsável apenas pelo gerenciamento da visão do grupo (membership). A ordenação das mensagens permanece totalmente descentralizada.

---

# 🧠 Visão Geral

O sistema é composto por um conjunto de peers que participam de um grupo distribuído.

Cada peer é responsável por:

- Enviar mensagens ao grupo;
- Propor números de sequência;
- Participar da definição da ordem final das mensagens;
- Entregar as mensagens na mesma ordem que todos os demais peers.

O líder eleito pelo algoritmo Bully possui apenas funções administrativas relacionadas ao grupo, como entrada, saída e detecção de falhas dos participantes.

---

# 🏗️ Arquitetura

```
                 +---------+
                 | Peer 1  |
                 +---------+
                /    |    \
               /     |     \
              /      |      \
      +---------+ +---------+ +---------+
      | Peer 2  | | Peer 3  | | Peer 4  |
      +---------+ +---------+ +---------+

        Todos executam o algoritmo ISIS
      O líder apenas mantém a visão do grupo
```

---

# ⚙️ Funcionamento

Quando um peer deseja enviar uma mensagem:

1. Captura a visão atual do grupo.
2. Envia a mensagem para todos os membros dessa visão.
3. Cada peer gera uma proposta de número de sequência.
4. O sender aguarda todas as propostas da visão capturada.
5. A maior proposta é escolhida como sequência final.
6. O sender envia o Agreement para todos os membros da mesma visão.
7. Cada peer marca a mensagem como entregável.
8. As mensagens são liberadas em ordem total.

A visão utilizada durante o envio permanece fixa até o término do protocolo daquela mensagem. Dessa forma, alterações no grupo durante a execução não afetam mensagens já iniciadas.

---

# 👥 Gerenciamento do Grupo

O gerenciamento da visão do grupo é independente do protocolo ISIS.

O algoritmo Bully é utilizado para:

- Eleição de líder;
- Entrada de novos peers;
- Remoção de peers que falharem;
- Manutenção de uma visão consistente do grupo.

Mudanças na visão afetam apenas mensagens futuras.

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
spring.application.name=peer

node.id=1
node.host=192.168.0.149

server.port=60000
```

Cada peer deve possuir:

- `node.id` único;
- Porta diferente;
- IP correspondente à máquina onde está executando.

---

# 🚀 Execução

Exemplo de inicialização dos peers:

```bash
java -jar peer.jar --node.id=1 --server.port=60000
```

```bash
java -jar peer.jar --node.id=2 --server.port=60001
```

```bash
java -jar peer.jar --node.id=3 --server.port=60002
```

```bash
java -jar peer.jar --node.id=4 --server.port=60003
```

Após iniciar, cada peer ingressa automaticamente no grupo por meio do serviço de gerenciamento.

---

# 📤 Exemplo de execução

Considere duas mensagens enviadas simultaneamente.

Peer 2 envia:

```
A
```

Peer 3 envia:

```
B
```

Cada peer gera sua proposta de sequência.

Após receber todas as propostas da visão utilizada no envio, o sender escolhe a maior sequência e envia o Agreement.

Todos os peers entregarão exatamente a mesma ordem.

Exemplo:

```
A
B
```

ou

```
B
A
```

A ordem pode variar entre execuções, mas será idêntica em todos os processos.

---

# ✅ Garantias

O sistema fornece:

- Ordem Total (Total Order)
- Entrega consistente entre todos os peers
- Ausência de sequenciador central
- Ordenação distribuída
- Gerenciamento consistente da visão do grupo
- Eleição automática de líder utilizando Bully

---

# ⚠️ Limitações

- O sender somente envia o Agreement após receber todas as propostas da visão utilizada durante o envio.
- Alterações na composição do grupo não interferem em mensagens já iniciadas.
- A nova visão passa a valer apenas para mensagens futuras.

---

# 📁 Estrutura do Projeto

```
controller/
dto/
model/
service/
utils/
```

Principais componentes:

- **ChatService** → Implementação do protocolo ISIS.
- **GroupService** → Gerenciamento da visão do grupo.
- **Bully** → Eleição de líder.

---

# 📝 Resumo

Este projeto implementa um Total Order Multicast baseado no algoritmo ISIS. A ordenação das mensagens ocorre de forma totalmente distribuída, sem um sequenciador central. O gerenciamento da visão do grupo é realizado separadamente utilizando o algoritmo Bully, garantindo que mudanças na composição do grupo não afetem mensagens que já estejam em processamento.
package chat.sda.spring.service;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import chat.sda.spring.dto.ChatMessageDTO;
import chat.sda.spring.model.Node;
import chat.sda.spring.model.OrderMessage;
import chat.sda.spring.utils.NodeConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SequencerService {

    private final AtomicLong sg;
    private final GroupService groupService;
    private final NodeConfig nodeConfig;
    private static final Logger log = LoggerFactory.getLogger(SequencerService.class);

    public SequencerService(GroupService groupService, NodeConfig nodeConfig) {

        this.sg = new AtomicLong(0);
        this.groupService = groupService;
        this.nodeConfig = nodeConfig;

    }

    public void bDeliver(ChatMessageDTO message) {

        long currentSg = sg.incrementAndGet();
        OrderMessage order = new OrderMessage(message.getId(),currentSg);
        bMulticast(order);
        log.info("Messagem - Id:{} - SenderId:{} - Conteudo:{}", message.getId(), message.getSenderId(), message.getContent());
        log.info("Sequencia Global Atual(SG):{}", currentSg);
        log.info("Ordem - Message Id:{} - SG:{}", order.getMessageId(), order.getSequenceNumber());

    }


    private void bMulticast(OrderMessage message) {

        ArrayList<Node> currentGroup = new ArrayList<>(groupService.getGroup());
        log.info("Mensagem de ordem - Message Id:{} - SG:{}", message.getMessageId(), message.getSequenceNumber());

        for (Node node : currentGroup) {

            if (node.equals(nodeConfig.getSelf())) {
                continue;
            }
            new Thread(() -> {
                try {

                    RestTemplate rest = new RestTemplate();

                    rest.postForEntity(
                            "http://" + node.getHost() + ":" + node.getPort() + "/chat/order",
                            message,
                            Void.class
                    );

                } catch (Exception e) {

                    System.out.println("Falha ao enviar para " + node.getId());
                }
            }).start();
        }
    }

}

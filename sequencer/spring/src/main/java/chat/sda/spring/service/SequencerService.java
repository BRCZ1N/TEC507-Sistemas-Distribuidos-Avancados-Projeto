package chat.sda.spring.service;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import chat.sda.spring.dto.ChatMessageDTO;
import chat.sda.spring.model.Node;
import chat.sda.spring.model.OrderMessage;
import chat.sda.spring.utils.NodeConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SequencerService {

    private final AtomicLong sg;
    private final GroupService groupService;
    private final NodeConfig nodeConfig;

    public SequencerService(GroupService groupService, NodeConfig nodeConfig) {

        this.sg = new AtomicLong(0);
        this.groupService = groupService;
        this.nodeConfig = nodeConfig;

    }

    public void bDeliver(ChatMessageDTO message) {

        long currentSg = sg.incrementAndGet();
        OrderMessage order = new OrderMessage(message.getId(),currentSg);
        bMulticast(order);
    }


    private void bMulticast(OrderMessage message) {

        ArrayList<Node> currentGroup = new ArrayList<>(groupService.getGroup());

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

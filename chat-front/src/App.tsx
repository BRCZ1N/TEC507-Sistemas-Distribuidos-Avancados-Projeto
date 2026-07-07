import { useEffect, useRef, useState } from "react";
import { Box, Typography, Paper, TextField, Button } from "@mui/material";

type Message = {
    id: string;
    senderId: string;
    content: string;
};

type Peer = {
    id: string;
    host: string;
    port: number;
};

const SEQUENCER_URL = "http://localhost:60000";

export default function App() {
    const [messages, setMessages] = useState<Message[]>([]);
    const [peers, setPeers] = useState<Peer[]>([]);
    const [selectedPeer, setSelectedPeer] = useState<Peer | null>(null);
    const [content, setContent] = useState("");

    const bottomRef = useRef<HTMLDivElement | null>(null);
    const previousMessageCount = useRef(0);

    const [senderId] = useState(() => {
        const saved = localStorage.getItem("guestId");

        if (saved) return saved;

        const id = `Guest-${Math.random().toString(36).substring(2, 8)}`;
        localStorage.setItem("guestId", id);

        return id;
    });


    const getPeers = async () => {
        try {
            const res = await fetch(`${SEQUENCER_URL}/group`);
            const data = await res.json();

            if (Array.isArray(data)) {
                setPeers(data);
            }

        } catch (err) {
            console.log("Erro getPeers:", err);
        }
    };


    const fetchMessages = async () => {
        if (!selectedPeer) return;

        try {
            const res = await fetch(
                `http://${selectedPeer.host}:${selectedPeer.port}/chat/messages`
            );

            if (!res.ok) return;

            const data = await res.json();

            if (Array.isArray(data)) {

                const changed =
                    data.length !== messages.length ||
                    data[data.length - 1]?.id !== messages[messages.length - 1]?.id;


                if (changed) {
                    setMessages(data);
                }
            }

        } catch (err) {
            console.log("fetch error:", err);
        }
    };


    const sendMessage = async () => {

        if (!content.trim() || !selectedPeer) return;


        try {

            await fetch(
                `http://${selectedPeer.host}:${selectedPeer.port}/chat/message`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify({
                        senderId,
                        content,
                    }),
                }
            );


            setContent("");

            fetchMessages();


        } catch (err) {

            console.log("send error:", err);

        }
    };


    useEffect(() => {
        getPeers();
    }, []);



    useEffect(() => {

        if (!selectedPeer) return;


        fetchMessages();


        const interval = setInterval(fetchMessages, 1200);


        return () => clearInterval(interval);


    }, [selectedPeer, messages]);



    useEffect(() => {

        if (messages.length > previousMessageCount.current) {

            bottomRef.current?.scrollIntoView({
                behavior: "smooth",
            });

        }


        previousMessageCount.current = messages.length;


    }, [messages]);



    return (

        <Box
            sx={{
                height: "100vh",
                width: "100vw",
                display: "flex",
                background: "linear-gradient(180deg, #0b0b0b, #151515)",
            }}
        >

            <Box
                sx={{
                    width: 220,
                    borderRight: "1px solid #222",
                    background: "#0f0f0f",
                    p: 2,
                    color: "white",
                }}
            >

                <Typography variant="caption">
                    Peers Online
                </Typography>


                <Box
                    sx={{
                        mt: 2,
                        display: "flex",
                        flexDirection: "column",
                        gap: 1
                    }}
                >

                    {peers.map((p) => (

                        <Box
                            key={p.id}
                            onClick={() => setSelectedPeer(p)}
                            sx={{
                                p: 1,
                                cursor: "pointer",
                                borderRadius: 1,
                                background:
                                    selectedPeer?.id === p.id
                                        ? "#2563eb"
                                        : "#1a1a1a",
                                "&:hover": {
                                    opacity: 0.8
                                },
                            }}
                        >

                            <Typography variant="caption">
                                {p.id}
                            </Typography>


                        </Box>

                    ))}


                </Box>


            </Box>



            <Box
                sx={{
                    flex: 1,
                    display: "flex",
                    justifyContent: "center",
                    alignItems: "center",
                }}
            >

                <Paper
                    elevation={10}
                    sx={{
                        width: "100%",
                        maxWidth: 720,
                        height: "90vh",
                        display: "flex",
                        flexDirection: "column",
                        borderRadius: 3,
                        overflow: "hidden",
                        background: "#121212",
                    }}
                >

                    <Box sx={{ p: 2, borderBottom: "1px solid #222" }}>

                        <Typography variant="caption" sx={{ color: "#fff" }}>
                            Total Order Multicast Chat
                        </Typography>


                        <Typography
                            variant="caption"
                            sx={{
                                color: "#888",
                                display: "block"
                            }}
                        >
                            Usuário: {senderId}
                        </Typography>


                        <Typography
                            variant="caption"
                            sx={{
                                color: "#888",
                                display: "block"
                            }}
                        >
                            Peer: {selectedPeer?.id ?? "nenhum"}
                        </Typography>


                    </Box>



                    <Box
                        sx={{
                            flex: 1,
                            overflowY: "auto",
                            p: 2,
                            display: "flex",
                            flexDirection: "column",
                            gap: 1.2,
                        }}
                    >

                        {messages.map((msg) => (

                            <Box
                                key={msg.id}
                                sx={{
                                    display: "flex"
                                }}
                            >

                                <Box
                                    sx={{
                                        maxWidth: "75%",
                                        p: "10px 12px",
                                        borderRadius: 2,
                                        background: "#2a2a2a",
                                        color: "white",
                                    }}
                                >

                                    <Typography
                                        variant="caption"
                                        sx={{
                                            opacity: 0.7
                                        }}
                                    >
                                        {msg.senderId}
                                    </Typography>


                                    <Typography variant="body2">
                                        {msg.content}
                                    </Typography>


                                </Box>


                            </Box>

                        ))}


                        <div ref={bottomRef}/>


                    </Box>



                    <Box
                        sx={{
                            display: "flex",
                            gap: 1,
                            p: 1.5,
                            borderTop: "1px solid #222",
                            background: "#1a1a1a",
                        }}
                    >

                        <TextField
                            fullWidth
                            size="small"
                            value={content}
                            onChange={(e) => setContent(e.target.value)}
                            onKeyDown={(e) => {

                                if (e.key === "Enter") {
                                    sendMessage();
                                }

                            }}
                            sx={{
                                input: {
                                    color: "white"
                                },

                                "& .MuiOutlinedInput-root": {
                                    color: "white",

                                    "& fieldset": {
                                        borderColor: "#333"
                                    },

                                },

                            }}
                        />


                        <Button
                            variant="contained"
                            onClick={sendMessage}
                            sx={{
                                background: "#2563eb",
                                textTransform: "none",
                            }}
                        >
                            Send
                        </Button>


                    </Box>


                </Paper>


            </Box>


        </Box>

    );
}
import { useEffect, useRef, useState } from "react";
import { Box, Typography, Paper, TextField, Button } from "@mui/material";

type Message = {
    id: string;
    senderId: string;
    content: string;
};

export default function App() {
    const [messages, setMessages] = useState<Message[]>([]);
    const [content, setContent] = useState("");

    const bottomRef = useRef<HTMLDivElement | null>(null);

    const [senderId] = useState(() => {
        const saved = localStorage.getItem("guestId");
        if (saved) return saved;

        const id = `Guest-${Math.random().toString(36).substring(2, 8)}`;
        localStorage.setItem("guestId", id);
        return id;
    });

    const joinGroup = async () => {
        try {
            await fetch("http://localhost:7000/group/join", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
            });
        } catch (err) {
            console.log("join error:", err);
        }
    };

    const fetchMessages = async () => {
        try {
            const res = await fetch("http://localhost:7000/chat");

            if (!res.ok) return;

            const data = await res.json();

            if (!Array.isArray(data)) return;

            setMessages(data);
        } catch (err) {
            console.log("fetch error:", err);
        }
    };

    const sendMessage = async () => {
        if (!content.trim()) return;

        try {
            await fetch("http://localhost:7000/chat/multicast", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    senderId,
                    content,
                }),
            });

            setContent("");

            // Atualiza logo após enviar
            fetchMessages();
        } catch (err) {
            console.log("send error:", err);
        }
    };

    useEffect(() => {
        joinGroup();
        fetchMessages();

        const interval = setInterval(fetchMessages, 1200);

        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        bottomRef.current?.scrollIntoView({
            behavior: "smooth",
        });
    }, [messages]);

    return (
        <Box
            sx={{
                height: "100vh",
                width: "100vw",
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
                background: "linear-gradient(180deg, #0b0b0b, #151515)",
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
                <Box
                    sx={{
                        p: 2,
                        borderBottom: "1px solid #222",
                        background: "#1a1a1a",
                    }}
                >
                    <Typography variant="caption" sx={{ color: "#fff" }}>
                        Total Order Multicast Chat
                    </Typography>

                    <Typography
                        variant="caption"
                        sx={{
                            color: "#888",
                            display: "block",
                        }}
                    >
                        Logado como {senderId}
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
                                display: "flex",
                                justifyContent: "flex-start",
                            }}
                        >
                            <Box
                                sx={{
                                    maxWidth: "75%",
                                    p: "10px 12px",
                                    borderRadius: 2,
                                    background: "#2a2a2a",
                                    color: "white",
                                    boxShadow: "0 2px 8px rgba(0,0,0,0.2)",
                                }}
                            >
                                <Typography
                                    variant="caption"
                                    sx={{
                                        opacity: 0.7,
                                        display: "block",
                                        mb: 0.5,
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

                    <div ref={bottomRef} />
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
                        placeholder="Digite uma mensagem..."
                        onChange={(e) => setContent(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key === "Enter") {
                                sendMessage();
                            }
                        }}
                        sx={{
                            input: {
                                color: "white",
                            },
                            "& .MuiOutlinedInput-root": {
                                color: "white",
                                "& fieldset": {
                                    borderColor: "#333",
                                },
                                "&:hover fieldset": {
                                    borderColor: "#555",
                                },
                                "&.Mui-focused fieldset": {
                                    borderColor: "#2563eb",
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
                            px: 3,
                        }}
                    >
                        Send
                    </Button>
                </Box>
            </Paper>
        </Box>
    );
}
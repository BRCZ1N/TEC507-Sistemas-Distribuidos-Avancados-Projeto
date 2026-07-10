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
    port: number | null;
};

const USE_HTTPS = false;

const SEQUENCER_URL = "http://192.168.1.107:60001";

const buildUrl = (host: string, port: number | null) => {

    if (USE_HTTPS) {
        return `https://${host}`;
    }

    return `http://${host}:${port}`;
};


export default function App() {

    const [messages, setMessages] = useState<Message[]>([]);
    const [peers, setPeers] = useState<Peer[]>([]);
    const [selectedPeer, setSelectedPeer] = useState<Peer | null>(null);

    const [content, setContent] = useState("");

    // Novo campo de delay em segundos
    const [delaySeconds, setDelaySeconds] = useState<number>(0);


    const bottomRef = useRef<HTMLDivElement | null>(null);
    const previousMessageCount = useRef(0);


    const [senderId] = useState(() => {

        const saved = localStorage.getItem("guestId");

        if (saved) return saved;

        const id = `Guest-${Math.random()
            .toString(36)
            .substring(2, 8)}`;

        localStorage.setItem("guestId", id);

        return id;
    });


    const getPeers = async () => {

        try {

            const res = await fetch(`${SEQUENCER_URL}/chat/nodes`);
            const data = await res.json();

            if (Array.isArray(data)) {
                setPeers(data);
            }

        } catch(err) {
            console.log(err);
        }

    };


    const fetchMessages = async () => {

        if (!selectedPeer) return;

        try {

            const res = await fetch(
                `${buildUrl(selectedPeer.host, selectedPeer.port)}/chat/messages`
            );

            if (!res.ok) return;

            const data = await res.json();

            if(Array.isArray(data)) {

                const changed =
                    data.length !== messages.length ||
                    data[data.length - 1]?.id !==
                    messages[messages.length - 1]?.id;


                if(changed){
                    setMessages(data);
                }
            }


        } catch(err){
            console.log(err);
        }

    };


    const sendMessage = async () => {

        if(!content.trim() || !selectedPeer)
            return;


        try {

            await fetch(
                `${buildUrl(selectedPeer.host, selectedPeer.port)}/chat/message`,
                {
                    method:"POST",

                    headers:{
                        "Content-Type":"application/json"
                    },

                    body:JSON.stringify({

                        senderId,

                        content,

                        // Conversão segundos -> milissegundos
                        artificialDelay: delaySeconds * 1000
                    })
                }
            );


            setContent("");

            fetchMessages();


        } catch(err){

            console.log(err);

        }

    };


    useEffect(()=>{

        getPeers();

    },[]);


    useEffect(()=>{

        if(!selectedPeer)
            return;


        fetchMessages();


        const timer =
            setInterval(fetchMessages,1200);


        return ()=>clearInterval(timer);


    },[selectedPeer,messages]);


    useEffect(()=>{

        if(messages.length > previousMessageCount.current){

            bottomRef.current?.scrollIntoView({
                behavior:"smooth"
            });

        }


        previousMessageCount.current =
            messages.length;


    },[messages]);

    return (

        <Box
            sx={{
                width:"100%",
                height:"100dvh",
                display:"flex",
                overflow:"hidden",
                background:
                    "linear-gradient(180deg,#0b0b0b,#151515)"
            }}
        >

            <Box
                sx={{
                    flex:"0 1 18%",
                    minWidth:0,
                    display:"flex",
                    flexDirection:"column",
                    p:2,
                    gap:2,
                    background:"#0f0f0f",
                    borderRight:"1px solid #222",
                    color:"white"
                }}
            >

                <Typography variant="caption">
                    Peers
                </Typography>


                <Box
                    sx={{
                        display:"flex",
                        flexDirection:"column",
                        gap:1,
                        overflow:"auto"
                    }}
                >

                    {
                        peers.map(peer=>(

                            <Box
                                key={peer.id}

                                onClick={()=>
                                    setSelectedPeer(peer)
                                }

                                sx={{
                                    p:1,
                                    cursor:"pointer",
                                    borderRadius:1,

                                    background:
                                        selectedPeer?.id===peer.id
                                            ?
                                            "#2563eb"
                                            :
                                            "#1a1a1a"
                                }}
                            >

                                <Typography variant="caption">
                                    {peer.id}
                                </Typography>

                            </Box>

                        ))
                    }

                </Box>

            </Box>


            <Box
                sx={{
                    flex:1,
                    minWidth:0,
                    display:"flex",
                    p:2
                }}
            >

                <Paper
                    sx={{
                        flex:1,
                        minWidth:0,
                        minHeight:0,
                        display:"flex",
                        flexDirection:"column",
                        overflow:"hidden",
                        borderRadius:2,
                        background:"#121212"
                    }}
                >


                    <Box
                        sx={{
                            p:2,
                            flexShrink:0,
                            borderBottom:"1px solid #222"
                        }}
                    >

                        <Typography
                            variant="caption"
                            sx={{
                                display:"block",
                                color:"#fff"
                            }}
                        >
                            Total Order Multicast Chat
                        </Typography>


                        <Typography
                            variant="caption"
                            sx={{
                                display:"block",
                                color:"#888"
                            }}
                        >
                            Usuário: {senderId}
                        </Typography>


                        <Typography
                            variant="caption"
                            sx={{
                                display:"block",
                                color:"#888"
                            }}
                        >
                            Peer: {selectedPeer?.id ?? "nenhum"}
                        </Typography>


                    </Box>



                    <Box
                        sx={{
                            flex:1,
                            minHeight:0,
                            overflowY:"auto",
                            p:2,
                            display:"flex",
                            flexDirection:"column",
                            gap:1
                        }}
                    >

                        {
                            messages.map(msg=>(

                                <Box
                                    key={msg.id}

                                    sx={{
                                        maxWidth:"80%",
                                        wordBreak:"break-word"
                                    }}
                                >

                                    <Box
                                        sx={{
                                            p:1.5,
                                            borderRadius:2,
                                            background:"#2a2a2a",
                                            color:"white"
                                        }}
                                    >

                                        <Typography
                                            variant="caption"
                                            sx={{
                                                opacity:.6
                                            }}
                                        >
                                            {msg.senderId}
                                        </Typography>


                                        <Typography variant="body2">
                                            {msg.content}
                                        </Typography>


                                    </Box>


                                </Box>

                            ))
                        }


                        <div ref={bottomRef}/>


                    </Box>



                    <Box
                        sx={{
                            display:"flex",
                            gap:1,
                            p:1.5,
                            flexShrink:0,
                            borderTop:"1px solid #222",
                            background:"#1a1a1a"
                        }}
                    >


                        <TextField
                            fullWidth

                            value={content}

                            onChange={
                                e=>setContent(e.target.value)
                            }

                            onKeyDown={
                                e=>{
                                    if(e.key==="Enter")
                                        sendMessage();
                                }
                            }


                            sx={{
                                input:{
                                    color:"white"
                                },

                                "& .MuiOutlinedInput-root":{
                                    color:"white",

                                    "& fieldset":{
                                        borderColor:"#333"
                                    }
                                }
                            }}
                        />



                        <TextField

                            type="number"

                            label="Delay (s)"

                            value={delaySeconds}


                            onChange={e=>{

                                const value =
                                    Number(e.target.value);


                                if(value >= 0 && value <= 30){

                                    setDelaySeconds(value);

                                }

                            }}


                            sx={{
                                width:120,


                                input:{
                                    color:"white"
                                },


                                "& .MuiOutlinedInput-root":{
                                    color:"white",

                                    "& fieldset":{
                                        borderColor:"#333"
                                    }
                                },


                                "& .MuiInputLabel-root":{
                                    color:"#888"
                                }

                            }}

                        />



                        <Button
                            variant="contained"

                            onClick={sendMessage}

                            sx={{
                                flex:"0 0 auto",
                                background:"#2563eb"
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
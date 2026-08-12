#docker run -d --name mcserver --restart=unless-stopped -e MEMORYSIZE="1G" -p 25565:25565/tcp -p 25565:25565/udp -v /home/docker/mcserver:/data:rw marctv/minecraft-papermc-server:26.2-111

docker start mcserver

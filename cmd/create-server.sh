docker run -d \
  --name mcserver \
  -it \
  -e MEMORYSIZE=2G \
  -v /home/docker/mcserver:/data \
  -p 25565:25565 \
  marctv/minecraft-papermc-server:26.2-111

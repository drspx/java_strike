# Java Strike 🎯

**En hurtig multiplayer shooter i Java** - bygget med ren Java og Swing.

## 🎮 Spilfunktioner

- **Multiplayer支持**: Vær vært eller joined spil via LAN
- **To spiltilstande**:
  - **Deathmatch**: Dødskamp til 60 sekunder. Flest drab vinder!
  - **Bomb Defusal**: Team-baseret bombefjernelse (Terrorister vs Counter-Terrorister)
- **Realistisk gameplay**: Bevægelige forhindringer, spawn-zoner, mållinje
- **FPS-overvågning**: Se din framerate i live

## 🚀 Hurtig Start

Kør spillet med følgende kommando:

```bash
java -jar java_strike.jar
```

**Systemkrav:**
- Java 11+ installeret
- Ca. 50MB ledig plads

## 🎯 Game Controls

| Key/Action | Beskrivelse |
|------------|-------------|
| **WASD** | Bevægelse |
| **Mouse** | Sigte |
| **Left Click** | Skyd |
| **Space** | Interaktion (bombefjernelse) |
| **R** | Restart match |
| **ESC** | Afslut |

## 🔧 Teknisk Information

**Arkitektur:**
- Ren Java Swing rendering
- TCP/IP netværk med 60 TPS server tick
- Single-threaded render loop
- Buffered rendering for optimal ydeevne

**Netværk:**
- Port: 4890 (konfigurerbar)
- 60 ticks per second server-side
- Client prediction og interpolation

## 📊 Ydelsesoptimeringer

Spillet er optimeret til høj FPS med:
- Disable antialiasing for maksimal ydeevne
- Simple rektangulær rendering i stedet for rounded corners
- Elimineret glow og shadow effekter
- Pre-cached colors og fonts
- Minimal objektoprettelse under gameplay

## 🛠 Byg

Byg projektet med Maven eller bygg dit jar direkte:

```bash
# From project root
mvn package

# Or run directly with javac/jar
```

## 📝 License

Privat projekt - alle rettigheder forbeholdes.

---

**Spil godt og hyggeligt!** 🎮

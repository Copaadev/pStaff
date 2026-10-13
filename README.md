# pStaff — Copa Development

Plugin de moderação para Paper 1.21.11, Java 21.

## Comandos

| Comando | Permissão | Função |
|---|---|---|
| `/staff` | `pstaff.staff` | Alterna modo staff, guardando e restaurando inventário, modo de jogo e voo |
| `/staff list` | `pstaff.staff` | Lista a equipe online |
| `/staff reload` | `pstaff.admin` | Recarrega config.yml |
| `/vanish` | `pstaff.vanish` | Invisibilidade para jogadores sem permissão staff |
| `/staffchat <mensagem>` | `pstaff.staff` | Chat privado da equipe (alias `/sc`) |
| `/invsee <jogador>` | `pstaff.invsee` | Visualiza cópia somente leitura do inventário |
| `/endersee <jogador>` | `pstaff.endersee` | Visualiza cópia somente leitura do Ender Chest |
| `/freeze <jogador>` e `/unfreeze <jogador>` | `pstaff.freeze` | Congela/descongela movimento |
| `/warn <jogador> <motivo>` | `pstaff.warn` | Envia aviso ao jogador |
| `/kick <jogador> <motivo>` | `pstaff.kick` | Expulsa jogador |
| `/mute <jogador> <tempo>` e `/unmute <jogador>` | `pstaff.mute` | Mute temporário (ex.: 10m, 2h, 1d) |
| `/heal [jogador]` | `pstaff.heal` | Restaura vida |
| `/feed [jogador]` | `pstaff.feed` | Restaura fome |
| `/fly [jogador]` | `pstaff.fly` | Alterna voo |
| `/god [jogador]` | `pstaff.god` | Alterna invulnerabilidade |
| `/clearchat` | `pstaff.clearchat` | Limpa o chat |
| `/workbench` | `pstaff.utility` | Abre bancada virtual (alias `/wb`) |
| `/trash` | `pstaff.utility` | Abre lixeira virtual |

Todas as permissões são `default: op`. Jogadores com `pstaff.admin` podem usar todos os comandos.

## Compilar no Arch Linux

Instale Java 21 e Maven:

```bash
sudo pacman -S jdk21-openjdk maven
```

Depois, na pasta do projeto:

```bash
chmod +x build.sh
./build.sh
```

O JAR será criado em `target/pStaff-1.0.0.jar`. Envie-o para a pasta `plugins/` e reinicie o servidor.

## Compatibilidade

Compilado para Java 21 e Paper API 1.21.11. O build precisa de acesso à internet na primeira execução para baixar a Paper API e os plugins Maven.

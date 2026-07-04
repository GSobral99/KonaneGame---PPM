Este projeto, desenvolvido no âmbito da Unidade Curricular de Programação Multiparadigma, foca-se
na implementação do jogo tradicional havaiano Konane utilizando a linguagem Scala e o paradigma de
programação funcional. O objetivo desta primeira fase criar a lógica do jogo Konane que seja suportada por
funções puras, imutabilidade e recursividade

A base técnica do trabalho assenta na utilização de tipos de dados imutáveis e coleções paralelas para
otimizar o processamento do tabuleiro. O tabuleiro é definido como um ParMap[Coord2D, Stone], onde as
coordenadas seguem o formato (row, column) e as peças são representadas por um enum contendo as cores Black e
White. Para a geração de números aleatórios, foi implementada uma abordagem funcional e pura através da
classe MyRandom, que evita efeitos secundários ao gerir o estado da semente de forma explícita.

No que diz respeito às funcionalidades obrigatórias (Tarefas T1 a T4), o grupo implementou com sucesso os seguintes componentes:
	Geração de Movimentos (T1): O método randomMove permite selecionar uma coordenada aleatória a partir
	de uma lista de posições livres, essencial para a automação de jogadas.

	Lógica de Jogo e Inicialização (T2): Foi desenvolvido o motor principal de execução de jogadas (play),
	que valida saltos simples e múltiplos, removendo as peças capturadas e atualizando o estado do tabuleiro.
	A inicialização respeita as regras do jogo, criando um padrão alternado e permitindo a remoção inicial de
	duas pedras adjacentes no centro ou nos cantos.

	Jogadas Automáticas (T3): Através de uma função de ordem superior, o sistema consegue realizar jogadas
	aleatórias válidas, demonstrando a flexibilidade do paradigma funcional ao passar comportamentos como argumentos.

	Visualização (T4): Foi criada uma interface textual (TUI) que imprime o estado do tabuleiro na consola,
	utilizando caracteres alfanuméricos para identificar linhas e colunas, facilitando a interação e o
	teste das funcionalidades.


-2ª Parte

Camada de Apresentação Textual (TUI - T7):
   - Menu inicial interativo em consola que permite configurar o jogo.
   - Suporte a jogos contra outro jogador ou contra o computador
   - Suporte a tabuleiros de tamanho personalizável e definição de
     limites de tempo por jogada.
   - Visualização clara do tabuleiro por caracteres alfanuméricos (T4).
   - Menu de ações em tempo de jogo: jogar, desfazer (Undo), salvar e ajuda.

Camada de Apresentação Gráfica (GUI - T8):
   - Desenvolvida com JavaFX e estruturada num único ficheiro FXML (KonaneApp.fxml)
     e um controlador unificado (AppController.scala)
   - Layout baseado em StackPane contendo múltiplas camadas dinâmicas geridas por
     visibilidade, prevenindo recriações desnecessárias de cenas.
   - Tabuleiro visual de dimensão fixa (6x6) renderizado dinamicamente via GridPane.
   - Usabilidade Avançada:
     * Seleção visual intuitiva com realce da peça selecionada (azul).
     * Deteção e indicação em tempo real de jogadas e destinos válidos (a azul claro).
     * Interação simplificada através do rato sem necessidade de input manual.

Gestão de Estado, Undo e Temporizador (T6):
   - Histórico de Estados: O estado completo do jogo é mantido numa lista imutável
     de `GameState`. A operação de Undo (Desfazer) simplesmente descarta a cabeça
     da lista, recuando no tempo sem efeitos secundários ou mutações de estado.
   - Temporizador Limite: Implementado via `Timeline` do JavaFX na GUI e através de
     verificação de marcas temporais (`System.currentTimeMillis()`) na TUI. Caso
     o tempo configurado (15s, 30s, 60s) se esgote, o turno passa ao adversário.
   - Capturas Múltiplas: Suporte total à interrupção voluntária ou continuação de
     saltos em cadeia através do botão "Parar Captura", respeitando
     as regras do Kōnane.

Modos de Jogo e Inteligência Artificial (Fatores de Valorização):
   - Modo Player vs Player (PvP) e Player vs Computer (PvE).
   - Três níveis de dificuldade implementados no Computador:
     1. Fácil: Escolha puramente aleatória de movimentos válidos (T3).
     2. Médio: Avalia o tabuleiro a um nível de profundidade e evita movimentos
        que reduzam drasticamente as suas próprias opções futuras.
     3. Difícil: Algoritmo que maximiza o número de peças capturadas
        na jogada (múltiplos saltos calculados de forma recursiva).

Camada de Dados (Salvar/Carregar Jogo):
   - Implementação de gravação e leitura do estado atual em ficheiros de texto (`.txt`)
     na raiz do projeto.
   - Grava o jogador atual, o limite de tempo, as dimensões e a disposição das
     peças (Board).
   - O ecrã de carregamento lê dinamicamente a pasta e preenche uma `ListView`
     para o utilizador escolher o save de forma interativa.
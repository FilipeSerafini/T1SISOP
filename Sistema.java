// PUCRS - Escola Politécnica - Sistemas Operacionais
// Prof. Fernando Dotti
// Código fornecido como parte da solução do projeto de Sistemas Operacionais
//
// VM
//    HW = memória, cpu
//    SW = tratamento int e chamada de sistema
// Funcionalidades de carga, execução e dump de memória

import java.util.*;

public class Sistema {
	
	// -------------------------------------------------------------------------------------------------------
	// --------------------- H A R D W A R E - definicoes de HW ---------------------------------------------- 

	// -------------------------------------------------------------------------------------------------------
	// --------------------- M E M O R I A -  definicoes de palavra de memoria, memória ---------------------- 
	
	public class Memory {
		public int tamMem;    
        public Word[] m;                  // m representa a memória fisica:   um array de posicoes de memoria (word)
	
		public Memory(int size){
			tamMem = size;
		    m = new Word[tamMem];      
		    for (int i=0; i<tamMem; i++) { m[i] = new Word(Opcode.___,-1,-1,-1); };
		}
		
		public void dump(Word w) {        // funcoes de DUMP nao existem em hardware - colocadas aqui para facilidade
						System.out.print("[ "); 
						System.out.print(w.opc); System.out.print(", ");
						System.out.print(w.r1);  System.out.print(", ");
						System.out.print(w.r2);  System.out.print(", ");
						System.out.print(w.p);  System.out.println("  ] ");
		}
		public void dump(int ini, int fim) {
			for (int i = ini; i < fim; i++) {		
				System.out.print(i); System.out.print(":  ");  dump(m[i]);
			}
		}
    }
	
    // -------------------------------------------------------------------------------------------------------

	public class Word { 	// cada posicao da memoria tem uma instrucao (ou um dado)
		public Opcode opc; 	//
		public int r1; 		// indice do primeiro registrador da operacao (Rs ou Rd cfe opcode na tabela)
		public int r2; 		// indice do segundo registrador da operacao (Rc ou Rs cfe operacao)
		public int p; 		// parametro para instrucao (k ou A cfe operacao), ou o dado, se opcode = DADO

		public Word(Opcode _opc, int _r1, int _r2, int _p) {  // vide definição da VM - colunas vermelhas da tabela
			opc = _opc;   r1 = _r1;    r2 = _r2;	p = _p;
		}
	}
	
	// -------------------------------------------------------------------------------------------------------
    // --------------------- C P U  -  definicoes da CPU ----------------------------------------------------- 

	public enum Opcode {
		DATA, ___,		                    // se memoria nesta posicao tem um dado, usa DATA, se nao usada ee NULO ___
		JMP, JMPI, JMPIG, JMPIL, JMPIE,     // desvios e parada
		JMPIM, JMPIGM, JMPILM, JMPIEM, STOP, 
		JMPIGK, JMPILK, JMPIEK, JMPIGT,     
		ADDI, SUBI, ADD, SUB, MULT,         // matematicos
		LDI, LDD, STD, LDX, STX, MOVE,      // movimentacao
        TRAP                                // chamada de sistema
	}

	public enum Interrupts {               // possiveis interrupcoes que esta CPU gera
		noInterrupt, intEnderecoInvalido, intInstrucaoInvalida, intOverflow, intSTOP;
	}

	public class CPU {
		private int maxInt; // valores maximo e minimo para inteiros nesta cpu
		private int minInt;
							// característica do processador: contexto da CPU ...
		private int pc; 			// ... composto de program counter,
		private Word ir; 			// instruction register,
		private int[] reg;       	// registradores da CPU
		private Interrupts irpt; 	// durante instrucao, interrupcao pode ser sinalizada
		private int base;   		// base e limite de acesso na memoria
		private int limite; // por enquanto toda memoria pode ser acessada pelo processo rodando
							// ATE AQUI: contexto da CPU - tudo que precisa sobre o estado de um processo para executa-lo
							// nas proximas versoes isto pode modificar

		private Memory mem;               // mem tem funcoes de dump e o array m de memória 'fisica' 
		private Word[] m;                 // CPU acessa MEMORIA, guarda referencia a 'm'. m nao muda. semre será um array de palavras

		private InterruptHandling ih;     // significa desvio para rotinas de tratamento de  Int - se int ligada, desvia
        private SysCallHandling sysCall;  // significa desvio para tratamento de chamadas de sistema - trap 
		private boolean debug;            // se true entao mostra cada instrucao em execucao
						
		public CPU(Memory _mem, InterruptHandling _ih, SysCallHandling _sysCall, boolean _debug) {     // ref a MEMORIA e interrupt handler passada na criacao da CPU
			maxInt =  32767;        // capacidade de representacao modelada
			minInt = -32767;        // se exceder deve gerar interrupcao de overflow
			mem = _mem;	            // usa mem para acessar funcoes auxiliares (dump)
			m = mem.m; 				// usa o atributo 'm' para acessar a memoria.
			reg = new int[10]; 		// aloca o espaço dos registradores - regs 8 e 9 usados somente para IO
			ih = _ih;               // aponta para rotinas de tratamento de int
            sysCall = _sysCall;     // aponta para rotinas de tratamento de chamadas de sistema
			debug =  _debug;        // se true, print da instrucao em execucao
		}
		
		private boolean legal(int e) {                             // todo acesso a memoria tem que ser verificado
			// ????
			return true;
		}

		private boolean testOverflow(int v) {                       // toda operacao matematica deve avaliar se ocorre overflow                      
			if ((v < minInt) || (v > maxInt)) {                       
				irpt = Interrupts.intOverflow;             
				return false;
			};
			return true;
		}
		
		public void setContext(int _base, int _limite, int _pc) {  // no futuro esta funcao vai ter que ser 
			base = _base;                                          // expandida para setar todo contexto de execucao,
			limite = _limite;									   // agora,  setamos somente os registradores base,
			pc = _pc;                                              // limite e pc (deve ser zero nesta versao)
			irpt = Interrupts.noInterrupt;                         // reset da interrupcao registrada
		}

		//Executa o processo até que uma interrupção ocorra.
		public void runProcess(int _base, int _limite, int _pc) {
			setContext(_base, _limite, _pc);
			run();
		}

		//Habilita o modo de depuração (debug) na CPU.
		public void traceOn() {
			debug = true;
		}

		//Desabilita o modo de depuração (debug) na CPU.
		public void traceOff() {
			debug = false;
		}

		//Define o modo de depuração (debug) conforme o valor passado.
		public void setTraceMode(boolean mode) {
			debug = mode;
		}
		
		public void run() { 		// execucao da CPU supoe que o contexto da CPU, vide acima, esta devidamente setado			
			while (true) { 			// ciclo de instrucoes. acaba cfe instrucao, veja cada caso.
			   // --------------------------------------------------------------------------------------------------
			   // FETCH
				if (legal(pc)) { 	// pc valido
					ir = m[pc]; 	// <<<<<<<<<<<<           busca posicao da memoria apontada por pc, guarda em ir
					if (debug) { System.out.print("                               pc: "+pc+"       exec: ");  mem.dump(ir); }
			   // --------------------------------------------------------------------------------------------------
			   // EXECUTA INSTRUCAO NO ir
					switch (ir.opc) {   // conforme o opcode (código de operação) executa

					// Instrucoes de Busca e Armazenamento em Memoria
					    case LDI: // Rd ← k
							reg[ir.r1] = ir.p;
							pc++;
							break;

						case LDD: // Rd <- [A]
						    if (legal(ir.p)) {
							   reg[ir.r1] = m[ir.p].p;
							   pc++;
						    }
						    break;

						case LDX: // RD <- [RS] // NOVA
							if (legal(reg[ir.r2])) {
								reg[ir.r1] = m[reg[ir.r2]].p;
								pc++;
							}
							break;

						case STD: // [A] ← Rs
						    if (legal(ir.p)) {
							    m[ir.p].opc = Opcode.DATA;
							    m[ir.p].p = reg[ir.r1];
							    pc++;
							};
						    break;

						case STX: // [Rd] ←Rs
						    if (legal(reg[ir.r1])) {
							    m[reg[ir.r1]].opc = Opcode.DATA;      
							    m[reg[ir.r1]].p = reg[ir.r2];          
								pc++;
							};
							break;
						
						case MOVE: // RD <- RS
							reg[ir.r1] = reg[ir.r2];
							pc++;
							break;	
							
					// Instrucoes Aritmeticas
						case ADD: // Rd ← Rd + Rs
							reg[ir.r1] = reg[ir.r1] + reg[ir.r2];
							testOverflow(reg[ir.r1]);
							pc++;
							break;

						case ADDI: // Rd ← Rd + k
							reg[ir.r1] = reg[ir.r1] + ir.p;
							testOverflow(reg[ir.r1]);
							pc++;
							break;

						case SUB: // Rd ← Rd - Rs
							reg[ir.r1] = reg[ir.r1] - reg[ir.r2];
							testOverflow(reg[ir.r1]);
							pc++;
							break;

						case SUBI: // RD <- RD - k // NOVA
							reg[ir.r1] = reg[ir.r1] - ir.p;
							testOverflow(reg[ir.r1]);
							pc++;
							break;

						case MULT: // Rd <- Rd * Rs
							reg[ir.r1] = reg[ir.r1] * reg[ir.r2];  
							testOverflow(reg[ir.r1]);
							pc++;
							break;

					// Instrucoes JUMP
						case JMP: // PC <- k
							pc = ir.p;
							break;
						
						case JMPIG: // If Rc > 0 Then PC ← Rs Else PC ← PC +1
							if (reg[ir.r2] > 0) {
								pc = reg[ir.r1];
							} else {
								pc++;
							}
							break;

						case JMPIGK: // If RC > 0 then PC <- k else PC++
							if (reg[ir.r2] > 0) {
								pc = ir.p;
							} else {
								pc++;
							}
							break;
	
						case JMPILK: // If RC < 0 then PC <- k else PC++
							 if (reg[ir.r2] < 0) {
								pc = ir.p;
							} else {
								pc++;
							}
							break;
	
						case JMPIEK: // If RC = 0 then PC <- k else PC++
								if (reg[ir.r2] == 0) {
									pc = ir.p;
								} else {
									pc++;
								}
							break;
	
	
						case JMPIL: // if Rc < 0 then PC <- Rs Else PC <- PC +1
								 if (reg[ir.r2] < 0) {
									pc = reg[ir.r1];
								} else {
									pc++;
								}
							break;
		
						case JMPIE: // If Rc = 0 Then PC <- Rs Else PC <- PC +1
								 if (reg[ir.r2] == 0) {
									pc = reg[ir.r1];
								} else {
									pc++;
								}
							break; 
	
						case JMPIM: // PC <- [A]
								 pc = m[ir.p].p;
							 break; 
	
						case JMPIGM: // If RC > 0 then PC <- [A] else PC++
								 if (reg[ir.r2] > 0) {
									pc = m[ir.p].p;
								} else {
									pc++;
								}
							 break;  
	
						case JMPILM: // If RC < 0 then PC <- k else PC++
								 if (reg[ir.r2] < 0) {
									pc = m[ir.p].p;
								} else {
									pc++;
								}
							 break; 
	
						case JMPIEM: // If RC = 0 then PC <- k else PC++
								if (reg[ir.r2] == 0) {
									pc = m[ir.p].p;
								} else {
									pc++;
								}
							 break; 
	
						case JMPIGT: // If RS>RC then PC <- k else PC++
								if (reg[ir.r1] > reg[ir.r2]) {
									pc = ir.p;
								} else {
									pc++;
								}
							 break; 

					// outras
						case STOP: // por enquanto, para execucao
							irpt = Interrupts.intSTOP;
							break;

						case DATA:
							irpt = Interrupts.intInstrucaoInvalida;
							break;

					// Chamada de sistema
					    case TRAP:
						     sysCall.handle();            // <<<<< aqui desvia para rotina de chamada de sistema, no momento so temos IO
							 pc++;
						     break;

					// Inexistente
						default:
							irpt = Interrupts.intInstrucaoInvalida;
							break;
					}
				}
			   // --------------------------------------------------------------------------------------------------
			   // VERIFICA INTERRUPÇÃO !!! - TERCEIRA FASE DO CICLO DE INSTRUÇÕES
				if (!(irpt == Interrupts.noInterrupt)) {   // existe interrupção
					ih.handle(irpt,pc);                       // desvia para rotina de tratamento
					break; // break sai do loop da cpu
				}
			}  // FIM DO CICLO DE UMA INSTRUÇÃO
		}      
	}
    // ------------------ C P U - fim ------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

    
	
    // ------------------- V M  - constituida de CPU e MEMORIA -----------------------------------------------
    // -------------------------- atributos e construcao da VM -----------------------------------------------
	public class VM {
		public int tamMem;    
        public Word[] m;  
		public Memory mem;   
        public CPU cpu;  
		public GP gp; 

		public GM_Paginada gm;
		public int tamPagOuPart;

		//public GM_Particionada gm;
		//public int tamPart;

        public VM(InterruptHandling ih, SysCallHandling sysCall){   
			// vm deve ser configurada com endereço de tratamento de interrupcoes e de chamadas de sistema
		    tamMem = 1024;
  		 	mem = new Memory(tamMem);
			m = mem.m;
			cpu = new CPU(mem,ih,sysCall, true); // true liga debug
			gp = new GP();

			tamPagOuPart = 16;
			gm = new GM_Paginada(tamMem, tamPagOuPart);

			//gm = new GM_Particionada(tamMem, tamPagOuPart);
			
	    }	
	}
    // ------------------- V M  - fim ------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

    // --------------------H A R D W A R E - fim -------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	
	// -------------------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------
	// ------------------- S O F T W A R E - inicio ----------------------------------------------------------

	// ------------------- I N T E R R U P C O E S  - rotinas de tratamento ----------------------------------
    public class InterruptHandling {
            public void handle(Interrupts irpt, int pc) {   // apenas avisa - todas interrupcoes neste momento finalizam o programa
				System.out.println("                                               Interrupcao "+ irpt+ "   pc: "+pc);
			}
	}

    // ------------------- C H A M A D A S  D E  S I S T E M A  - rotinas de tratamento ----------------------
    public class SysCallHandling {
        private VM vm;
        public void setVM(VM _vm){
            vm = _vm;
        }
        public void handle() {   // apenas avisa - todas interrupcoes neste momento finalizam o programa
            System.out.println("                                               Chamada de Sistema com op  /  par:  "+ vm.cpu.reg[8] + " / " + vm.cpu.reg[9]);
		}
    }

    // ------------------ U T I L I T A R I O S   D O   S I S T E M A -----------------------------------------
	// ------------------ load é invocado a partir de requisição do usuário

	private void loadProgram(Word[] p, Word[] m) {
		for (int i = 0; i < p.length; i++) {
			m[i].opc = p[i].opc;     m[i].r1 = p[i].r1;     m[i].r2 = p[i].r2;     m[i].p = p[i].p;
		}
	}

	private void loadProgram(Word[] p) {
		loadProgram(p, vm.m);
	}

	private void loadAndExec(Word[] p){
		loadProgram(p);    // carga do programa na memoria
				System.out.println("---------------------------------- programa carregado na memoria");
				vm.mem.dump(0, p.length);            // dump da memoria nestas posicoes				
		vm.cpu.setContext(0, vm.tamMem - 1, 0);      // seta estado da cpu ]
				System.out.println("---------------------------------- inicia execucao ");
		vm.cpu.run();                                // cpu roda programa ate parar	
				System.out.println("---------------------------------- memoria após execucao ");
				vm.mem.dump(0, p.length);            // dump da memoria com resultado
	}

	// -------------------------------------------------------------------------------------------------------
    // -------------------  S I S T E M A --------------------------------------------------------------------

    private static Scanner scanner = new Scanner(System.in);
    private static VM vm;
    private InterruptHandling ih;
    private SysCallHandling sysCall;
    private Programas progs;
	

    public Sistema(){   // a VM com tratamento de interrupções
        ih = new InterruptHandling();
        sysCall = new SysCallHandling();
        vm = new VM(ih, sysCall);
        sysCall.setVM(vm);
        progs = new Programas();
	}

    // -------------------  S I S T E M A - fim --------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------
    // ------------------- instancia e testa sistema
	public static void main(String args[]) {
		Sistema s = new Sistema();			
		//s.loadAndExec(s.progs.fibonacci10);
		//s.loadAndExec(s.progs.progMinimo);

		s.loadAndExec(s.progs.fatorial);
		//s.loadAndExec(s.progs.fatorialTRAP); // saida
		//s.loadAndExec(s.progs.fibonacciTRAP); // entrada
		//s.loadAndExec(s.progs.PC); // bubble sort

		executaMenu();
	}

	public static void showMenu() {
		System.out.println("\n===============MENU===============");
		System.out.println("Escolha a opcao desejada: ");
		System.out.println("1: Criar processo");
		System.out.println("2: Remover processo");
		System.out.println("3: Listar todos processos");
		System.out.println("4: Listar conteudo do PCB e o conteudo da memoria do processo com id");
		System.out.println("5: Listar a memoria entre posicoes inicio e fim, independente do processo");
		System.out.println("6: Executar processo a partir de um ID");
		System.out.println("7: Ligar modo de execucao em que CPU print cada instrucao executada");
		System.out.println("8: Desligar o modo de execução acima");
		System.out.println("9: Sair do sistema");
	}

	
	public static void executaMenu() {
		int opcao = 0;
		do {
			showMenu();
			opcao = scanner.nextInt();
			switch (opcao) {
				case 1:
					//Criar processo
					System.out.println("Digite o tamanho do processo que deseja criar:");
					int tamanhoProcesso = scanner.nextInt();

					//boolean processoCriado = vm.gp.criaProcessoParticionada(tamanhoProcesso);
					boolean processoCriado = vm.gp.criaProcessoPaginada(tamanhoProcesso);
					
					if(processoCriado) {
						System.out.println("Processo criado com sucesso!");
					} else {
						System.out.println("Erro ao criar o processo. Memória insuficiente ou outro problema.");
					}
					break;
				case 2:
					//Remover processo
					System.out.println("Digite o ID do processo que deseja remover:");
					int idProcesso = scanner.nextInt();
					boolean processoRemovido = vm.gp.desalocaProcesso(idProcesso);
					if(processoRemovido) {
						System.out.println("Processo removido com sucesso!");
					} else {
						System.out.println("Erro ao remover o processo. ID inválido ou outro problema.");
					}
					break;
				case 3:
					//Listar todos processos
					vm.gp.listaProcessos();
					break;	
				case 4:
					//Listar conteudo do PCB e o conteúdo da memoria do processo com id
					System.out.println("Digite o ID do processo que deseja listar:");
					int idProcessoListar = scanner.nextInt();
					vm.gp.dump(idProcessoListar);
					break;	
				case 5:
					//Listar a memoria entre posicoes inicio e fim, independente do processo
					vm.gm.dumpMemoria(0, vm.gm.qtdPartOuPag - 1);
					break;
				case 6:
					//Executar processo a partir de um ID
					System.out.println("Digite o ID do processo que deseja executar:");
					int idProcessoExecutar = scanner.nextInt();
					vm.gp.executa(idProcessoExecutar);
					break;	
				case 7:
					//Ligar modo de execução em que CPU print cada instrução executada
					vm.cpu.traceOn();
					System.out.println("Modo de execução ligado.");
					break;	
				case 8:
					//Desligar o modo de execução acima
					vm.cpu.traceOff();
					System.out.println("Modo de execução desligado.");
					break;	
				case 9:
					//Sair do sistema
					System.out.println("Tchau!");
					break;	
				default:
					System.out.println("Opcao inválida! Redigite.");
			}
		} while(opcao != 9);
	}
	

	public class GM_Particionada {

		int tamMem;
		int tamPart;
		int qtdPartOuPag;

		//array de boolean para representar a memoria, FALSE eh posicao LIVRE, TRUE eh posicao OCUPADA
		boolean[] memoria;

		public GM_Particionada(int tamanhoMemoria, int tamanhoParticao) {
			tamMem = tamanhoMemoria;
			tamPart = tamanhoParticao;
			qtdPartOuPag = tamMem/tamPart;

			memoria = new boolean[qtdPartOuPag];
		}

		public int aloca(int tamProg) {
			if (tamProg <= tamPart) {
				for (int i = 0; i < memoria.length; i++) {
					if (memoria[i] == false) {
						memoria[i] = true;
						return i;
					}
				}
			}

			return -1;
		}

		public void desaloca(int[] particao) {
			//variavel particao vai ser um array com apenas uma posicao, contendo a particao em que o programa esta alocado
			memoria[particao[0]] = false;
		}

		public void dumpMemoria(int inicio, int fim) {
			System.out.print("[ ");
			for (int i = inicio; i <= fim; i++) {
				System.out.print(memoria[i] + " ");
			}
			System.out.println("]");
		}
		
	}

	public class GM_Paginada {

		int tamMem;
		int tamPag;
		int qtdPartOuPag;
		int qtdPaginasLivres;

		boolean[] memoria;

		
		public GM_Paginada(int tamanhoMemoria, int tamanhoPagina) {
			tamMem = tamanhoMemoria;
			tamPag = tamanhoPagina;

			qtdPartOuPag = tamMem/tamPag;
			qtdPaginasLivres = qtdPartOuPag;
			memoria = new boolean[qtdPartOuPag];
		}

		public boolean aloca(int qtdPaginas, int[] tabelaPaginas) {
			if (qtdPaginas <= qtdPaginasLivres && qtdPaginas <= tabelaPaginas.length) {
				int indexTabelaPaginas = 0;
				for (int i = 0; i < memoria.length; i++) {
					if (memoria[i] == false) {
						memoria[i] = true;
						qtdPaginasLivres--;
						tabelaPaginas[indexTabelaPaginas] = i;
						indexTabelaPaginas++;
						if (indexTabelaPaginas >= tabelaPaginas.length) {
							break;
						}
					}
				}
				return true;
			}
			return false;
		}

		public void desaloca(int[] tabelaPaginas) {
			for(int i = 0; i < tabelaPaginas.length; i++) {
				memoria[tabelaPaginas[i]] = false;
			}
		}

		public void dumpMemoria(int inicio, int fim) {
			// Validando as entradas
			if (inicio < 0 || fim >= memoria.length || inicio > fim) {
				System.out.println("Posições inválidas!");
				return;
			}
		
			System.out.print("[ ");
			for (int i = inicio; i <= fim; i++) {
				System.out.print(memoria[i] + " ");
			}
			System.out.println(" ]");
		}
		
	}

	public class GP {

		ArrayList<PCB> listaPCB = new ArrayList<>();
		int idPCB = 0;

		public GP() {
		}

		public boolean criaProcessoPaginada(int tamanhoProcesso) {
			//tamanhoProcesso serve tanto para para paginacao ou particao, se for 1 eh ou uma pagina ou particao, se for maior eh paginas 
			
			int[] tabelaPaginas = new int[tamanhoProcesso];
			boolean conseguiuAlocar = vm.gm.aloca(tamanhoProcesso, tabelaPaginas);

			//pensar como vamos verificar se conseguiu alocar
			if(conseguiuAlocar) {
				idPCB++;
				PCB pcb = new PCB(tamanhoProcesso, idPCB, tabelaPaginas);
				listaPCB.add(pcb);
				return true;
			} else {
				return false;
			}
		}

		// public boolean criaProcessoParticionada(int tamanhoProcesso) {

		// 	int[] particaoAlocada = new int[1];
		// 	particaoAlocada[0] = vm.gm.aloca(tamanhoProcesso);

		// 	if (particaoAlocada[0] > -1) {
		// 		idPCB++;
		// 		int pc = 0;
		// 		PCB pcb = new PCB(tamanhoProcesso, idPCB, particaoAlocada);
		// 		listaPCB.add(pcb);

		// 		return true;

		// 	} else {
		// 		return false;
		// 	}
		// }

		//desalocar processo
		public boolean desalocaProcesso(int idProcesso) {
			Iterator<PCB> iterator = listaPCB.iterator();
			while (iterator.hasNext()) {
				PCB pcb = iterator.next();
				if (pcb.id == idProcesso) {
					vm.gm.desaloca(pcb.particoesOuPaginas);
					iterator.remove();  // Remove o PCB da lista.
					return true;
				}
			}
			return false;
		}

		//listar todos PCBs
		public void listaProcessos() {
			if(listaPCB.isEmpty()) {
				System.out.println("\nNão há processos na lista.");
			}
			else {
				System.out.println("\nLista de processos: ");
				for (PCB pcb : listaPCB) {
					System.out.println(pcb.toString());
				}
			}
		}

		//lista o conteúdo do PCB e o conteúdo da partição de memória do processo com id
		public void dump(int id) {
			boolean encontrou = false;
			for (PCB pcb : listaPCB) {
				if (pcb.id == id) {
					encontrou = true;
					System.out.println("\n" + pcb.toString());
		
					// Mostrando a memória:
					for (int i : pcb.particoesOuPaginas) {
						System.out.println("Endereço: " + i + " - Valor: " + vm.m[i]);
					}
				}
			}
			if(!encontrou) {
				System.out.println("Não foi encontrado processo com o ID informado.");
			}
		}

		//executa o processo com id fornecido. se não houver processo, retorna erro.
		public void executa(int id) {
			boolean encontrou = false;
			for (PCB pcb : listaPCB) {
				if (pcb.id == id) {
					encontrou = true;
					int base = pcb.particoesOuPaginas[0];
					int limite = pcb.particoesOuPaginas[pcb.particoesOuPaginas.length - 1];
					vm.cpu.runProcess(base, limite, pcb.pc);
					break;
				}
			}
			if(!encontrou) {
				System.out.println("Não foi encontrado processo com o ID informado.");
			}
		}

		//liga modo de execução em que CPU print cada instrução executada
		public void traceOn() {
			vm.cpu.setTraceMode(true);
		}

		//desliga o modo acima
		public void traceOff() {
			vm.cpu.setTraceMode(false);
		}

	}

	public class PCB {
		int qtdPaginasOuParticoes;
		int id;
		int[] particoesOuPaginas;
		int pc;

		public PCB(int qtdPaginasOuParticoes, int id, int[] tabelaPaginasOuParticao) {
			this.qtdPaginasOuParticoes = qtdPaginasOuParticoes;
			this.id = id;
			this.particoesOuPaginas = tabelaPaginasOuParticao;
			this.pc = 0;
		}

		@Override
		public String toString() {
			return "PCB [ id: " + id +
				   ",\nquantidade particoes ou paginas: " + qtdPaginasOuParticoes + 
				   ",\nparticoes ou paginas alocadas: " + Arrays.toString(particoesOuPaginas) + 
				   "\n]";
		}
	}


   // -------------------------------------------------------------------------------------------------------
   // -------------------------------------------------------------------------------------------------------
   // -------------------------------------------------------------------------------------------------------
   // --------------- P R O G R A M A S  - não fazem parte do sistema
   // esta classe representa programas armazenados (como se estivessem em disco) 
   // que podem ser carregados para a memória (load faz isto)

   public class Programas {
	   public Word[] fatorial = new Word[] {
	 	           // este fatorial so aceita valores positivos.   nao pode ser zero
	 											 // linha   coment
	 		new Word(Opcode.LDI, 0, -1, 4),      // 0   	r0 é valor a calcular fatorial
	 		new Word(Opcode.LDI, 1, -1, 1),      // 1   	r1 é 1 para multiplicar (por r0)
	 		new Word(Opcode.LDI, 6, -1, 1),      // 2   	r6 é 1 para ser o decremento
	 		new Word(Opcode.LDI, 7, -1, 8),      // 3   	r7 tem posicao de stop do programa = 8
	 		new Word(Opcode.JMPIE, 7, 0, 0),     // 4   	se r0=0 pula para r7(=8)
			new Word(Opcode.MULT, 1, 0, -1),     // 5   	r1 = r1 * r0
	 		new Word(Opcode.SUB, 0, 6, -1),      // 6   	decrementa r0 1 
	 		new Word(Opcode.JMP, -1, -1, 4),     // 7   	vai p posicao 4
	 		new Word(Opcode.STD, 1, -1, 10),     // 8   	coloca valor de r1 na posição 10
	 		new Word(Opcode.STOP, -1, -1, -1),   // 9   	stop
	 		new Word(Opcode.DATA, -1, -1, -1) }; // 10   ao final o valor do fatorial estará na posição 10 da memória                                    
		
	   public Word[] progMinimo = new Word[] {
		    new Word(Opcode.LDI, 0, -1, 999), 		
			new Word(Opcode.STD, 0, -1, 10), 
			new Word(Opcode.STD, 0, -1, 11), 
			new Word(Opcode.STD, 0, -1, 12), 
			new Word(Opcode.STD, 0, -1, 13), 
			new Word(Opcode.STD, 0, -1, 14), 
			new Word(Opcode.STOP, -1, -1, -1) };

	   public Word[] fibonacci10 = new Word[] { // mesmo que prog exemplo, so que usa r0 no lugar de r8
			new Word(Opcode.LDI, 1, -1, 0), 
			new Word(Opcode.STD, 1, -1, 20),   
			new Word(Opcode.LDI, 2, -1, 1),
			new Word(Opcode.STD, 2, -1, 21),  
			new Word(Opcode.LDI, 0, -1, 22),  
			new Word(Opcode.LDI, 6, -1, 6),
			new Word(Opcode.LDI, 7, -1, 31),  
			new Word(Opcode.LDI, 3, -1, 0), 
			new Word(Opcode.ADD, 3, 1, -1),
			new Word(Opcode.LDI, 1, -1, 0), 
			new Word(Opcode.ADD, 1, 2, -1), 
			new Word(Opcode.ADD, 2, 3, -1),
			new Word(Opcode.STX, 0, 2, -1), 
			new Word(Opcode.ADDI, 0, -1, 1), 
			new Word(Opcode.SUB, 7, 0, -1),
			new Word(Opcode.JMPIG, 6, 7, -1), 
			new Word(Opcode.STOP, -1, -1, -1), 
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),   // POS 20
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1) }; // ate aqui - serie de fibonacci ficara armazenada
		
       public Word[] fatorialTRAP = new Word[] {
		   new Word(Opcode.LDI, 0, -1, 7),// numero para colocar na memoria
		   new Word(Opcode.STD, 0, -1, 50),
		   new Word(Opcode.LDD, 0, -1, 50),
		   new Word(Opcode.LDI, 1, -1, -1),
		   new Word(Opcode.LDI, 2, -1, 13),// SALVAR POS STOP
           new Word(Opcode.JMPIL, 2, 0, -1),// caso negativo pula pro STD
           new Word(Opcode.LDI, 1, -1, 1),
           new Word(Opcode.LDI, 6, -1, 1),
           new Word(Opcode.LDI, 7, -1, 13),
           new Word(Opcode.JMPIE, 7, 0, 0), //POS 9 pula pra STD (Stop-1)
           new Word(Opcode.MULT, 1, 0, -1),
           new Word(Opcode.SUB, 0, 6, -1),
           new Word(Opcode.JMP, -1, -1, 9),// pula para o JMPIE
           new Word(Opcode.STD, 1, -1, 18),
           new Word(Opcode.LDI, 8, -1, 2),// escrita
           new Word(Opcode.LDI, 9, -1, 18),//endereco com valor a escrever
           new Word(Opcode.TRAP, -1, -1, -1),
           new Word(Opcode.STOP, -1, -1, -1), // POS 17
           new Word(Opcode.DATA, -1, -1, -1)  };//POS 18	
		   
	       public Word[] fibonacciTRAP = new Word[] { // mesmo que prog exemplo, so que usa r0 no lugar de r8
			new Word(Opcode.LDI, 8, -1, 1),// leitura
			new Word(Opcode.LDI, 9, -1, 100),//endereco a guardar
			new Word(Opcode.TRAP, -1, -1, -1),
			new Word(Opcode.LDD, 7, -1, 100),// numero do tamanho do fib
			new Word(Opcode.LDI, 3, -1, 0),
			new Word(Opcode.ADD, 3, 7, -1),
			new Word(Opcode.LDI, 4, -1, 36),//posicao para qual ira pular (stop) *
			new Word(Opcode.LDI, 1, -1, -1),// caso negativo
			new Word(Opcode.STD, 1, -1, 41),
			new Word(Opcode.JMPIL, 4, 7, -1),//pula pra stop caso negativo *
			new Word(Opcode.JMPIE, 4, 7, -1),//pula pra stop caso 0
			new Word(Opcode.ADDI, 7, -1, 41),// fibonacci + posição do stop
			new Word(Opcode.LDI, 1, -1, 0),
			new Word(Opcode.STD, 1, -1, 41),    // 25 posicao de memoria onde inicia a serie de fibonacci gerada
			new Word(Opcode.SUBI, 3, -1, 1),// se 1 pula pro stop
			new Word(Opcode.JMPIE, 4, 3, -1),
			new Word(Opcode.ADDI, 3, -1, 1),
			new Word(Opcode.LDI, 2, -1, 1),
			new Word(Opcode.STD, 2, -1, 42),
			new Word(Opcode.SUBI, 3, -1, 2),// se 2 pula pro stop
			new Word(Opcode.JMPIE, 4, 3, -1),
			new Word(Opcode.LDI, 0, -1, 43),
			new Word(Opcode.LDI, 6, -1, 25),// salva posição de retorno do loop
			new Word(Opcode.LDI, 5, -1, 0),//salva tamanho
			new Word(Opcode.ADD, 5, 7, -1),
			new Word(Opcode.LDI, 7, -1, 0),//zera (inicio do loop)
			new Word(Opcode.ADD, 7, 5, -1),//recarrega tamanho
			new Word(Opcode.LDI, 3, -1, 0),
			new Word(Opcode.ADD, 3, 1, -1),
			new Word(Opcode.LDI, 1, -1, 0),
			new Word(Opcode.ADD, 1, 2, -1),
			new Word(Opcode.ADD, 2, 3, -1),
			new Word(Opcode.STX, 0, 2, -1),
			new Word(Opcode.ADDI, 0, -1, 1),
			new Word(Opcode.SUB, 7, 0, -1),
			new Word(Opcode.JMPIG, 6, 7, -1),//volta para o inicio do loop
			new Word(Opcode.STOP, -1, -1, -1),   // POS 36
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),   // POS 41
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1),
			new Word(Opcode.DATA, -1, -1, -1)
	};

	public Word[] PB = new Word[] {
		//dado um inteiro em alguma posição de memória,
		// se for negativo armazena -1 na saída; se for positivo responde o fatorial do número na saída
		new Word(Opcode.LDI, 0, -1, 7),// numero para colocar na memoria
		new Word(Opcode.STD, 0, -1, 50),
		new Word(Opcode.LDD, 0, -1, 50),
		new Word(Opcode.LDI, 1, -1, -1),
		new Word(Opcode.LDI, 2, -1, 13),// SALVAR POS STOP
		new Word(Opcode.JMPIL, 2, 0, -1),// caso negativo pula pro STD
		new Word(Opcode.LDI, 1, -1, 1),
		new Word(Opcode.LDI, 6, -1, 1),
		new Word(Opcode.LDI, 7, -1, 13),
		new Word(Opcode.JMPIE, 7, 0, 0), //POS 9 pula pra STD (Stop-1)
		new Word(Opcode.MULT, 1, 0, -1),
		new Word(Opcode.SUB, 0, 6, -1),
		new Word(Opcode.JMP, -1, -1, 9),// pula para o JMPIE
		new Word(Opcode.STD, 1, -1, 15),
		new Word(Opcode.STOP, -1, -1, -1), // POS 14
		new Word(Opcode.DATA, -1, -1, -1)}; //POS 15

public Word[] PC = new Word[] {
		//Para um N definido (10 por exemplo)
		//o programa ordena um vetor de N números em alguma posição de memória;
		//ordena usando bubble sort
		//loop ate que não swap nada
		//passando pelos N valores
		//faz swap de vizinhos se da esquerda maior que da direita
		new Word(Opcode.LDI, 7, -1, 5),// TAMANHO DO BUBBLE SORT (N)
		new Word(Opcode.LDI, 6, -1, 5),//aux N
		new Word(Opcode.LDI, 5, -1, 46),//LOCAL DA MEMORIA
		new Word(Opcode.LDI, 4, -1, 47),//aux local memoria
		new Word(Opcode.LDI, 0, -1, 4),//colocando valores na memoria
		new Word(Opcode.STD, 0, -1, 46),
		new Word(Opcode.LDI, 0, -1, 3),
		new Word(Opcode.STD, 0, -1, 47),
		new Word(Opcode.LDI, 0, -1, 5),
		new Word(Opcode.STD, 0, -1, 48),
		new Word(Opcode.LDI, 0, -1, 1),
		new Word(Opcode.STD, 0, -1, 49),
		new Word(Opcode.LDI, 0, -1, 2),
		new Word(Opcode.STD, 0, -1, 50),//colocando valores na memoria até aqui - POS 13
		new Word(Opcode.LDI, 3, -1, 25),// Posicao para pulo CHAVE 1
		new Word(Opcode.STD, 3, -1, 99),
		new Word(Opcode.LDI, 3, -1, 22),// Posicao para pulo CHAVE 2
		new Word(Opcode.STD, 3, -1, 98),
		new Word(Opcode.LDI, 3, -1, 38),// Posicao para pulo CHAVE 3
		new Word(Opcode.STD, 3, -1, 97),
		new Word(Opcode.LDI, 3, -1, 25),// Posicao para pulo CHAVE 4 (não usada)
		new Word(Opcode.STD, 3, -1, 96),
		new Word(Opcode.LDI, 6, -1, 0),//r6 = r7 - 1 POS 22
		new Word(Opcode.ADD, 6, 7, -1),
		new Word(Opcode.SUBI, 6, -1, 1),//ate aqui
		new Word(Opcode.JMPIEM, -1, 6, 97),//CHAVE 3 para pular quando r7 for 1 e r6 0 para interomper o loop de vez do programa
		new Word(Opcode.LDX, 0, 5, -1),//r0 e r1 pegando valores das posições da memoria POS 26
		new Word(Opcode.LDX, 1, 4, -1),
		new Word(Opcode.LDI, 2, -1, 0),
		new Word(Opcode.ADD, 2, 0, -1),
		new Word(Opcode.SUB, 2, 1, -1),
		new Word(Opcode.ADDI, 4, -1, 1),
		new Word(Opcode.SUBI, 6, -1, 1),
		new Word(Opcode.JMPILM, -1, 2, 99),//LOOP chave 1 caso neg procura prox
		new Word(Opcode.STX, 5, 1, -1),
		new Word(Opcode.SUBI, 4, -1, 1),
		new Word(Opcode.STX, 4, 0, -1),
		new Word(Opcode.ADDI, 4, -1, 1),
		new Word(Opcode.JMPIGM, -1, 6, 99),//LOOP chave 1 POS 38
		new Word(Opcode.ADDI, 5, -1, 1),
		new Word(Opcode.SUBI, 7, -1, 1),
		new Word(Opcode.LDI, 4, -1, 0),//r4 = r5 + 1 POS 41
		new Word(Opcode.ADD, 4, 5, -1),
		new Word(Opcode.ADDI, 4, -1, 1),//ate aqui
		new Word(Opcode.JMPIGM, -1, 7, 98),//LOOP chave 2
		new Word(Opcode.STOP, -1, -1, -1), // POS 45
		new Word(Opcode.DATA, -1, -1, -1),
		new Word(Opcode.DATA, -1, -1, -1),
		new Word(Opcode.DATA, -1, -1, -1),
		new Word(Opcode.DATA, -1, -1, -1),
		new Word(Opcode.DATA, -1, -1, -1),
		new Word(Opcode.DATA, -1, -1, -1),
		new Word(Opcode.DATA, -1, -1, -1),
		new Word(Opcode.DATA, -1, -1, -1)};
   }
}


package Modelo;

import Controle.Comunicador;
import Recursos.Arquivo;
import Recursos.Numero;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Jonatas Trabuco Belotti [jonatas.t.belotti@hotmail.com]
 */
public class RBF {

  public static final int NUM_ENTRADAS = 2;
  private final int NUM_NEU_CAMADA_ESCONDIDA = 2;
  public static final int NUM_NEU_CAMADA_SAIDA = 1;
  private final double LIMIAR_ATIVACAO = -1D;
  private final double TAXA_APRENDIZADO = 0.01;
  private final double PRECISAO = Math.pow(10D, -7D);

  private double[] entradas;
  private double[] saidasEsperadas;
  private double[] variancia;
  private double[][] pesosCamadaEscondida;
  private double[][] pesosCamadaSaida;
  private double[] saidaCamadaEscondida;
  private double[] saidaCamadaSaida;
  private int numEpocas;

  public RBF() {
    this.entradas = new double[NUM_ENTRADAS];
    this.variancia = new double[NUM_NEU_CAMADA_ESCONDIDA];
    this.pesosCamadaEscondida = new double[NUM_NEU_CAMADA_ESCONDIDA][NUM_ENTRADAS];
    this.pesosCamadaSaida = new double[NUM_NEU_CAMADA_SAIDA][NUM_NEU_CAMADA_ESCONDIDA + 1];//+1 por causa do peso do limiar de ativacao
    this.saidaCamadaEscondida = new double[NUM_NEU_CAMADA_ESCONDIDA + 1];//+1 por causa do limiar de ativacao
    this.saidaCamadaSaida = new double[NUM_NEU_CAMADA_SAIDA];
    this.saidasEsperadas = new double[NUM_NEU_CAMADA_SAIDA];
  }

  public boolean treinar(Arquivo arquivoTreinamento) {
    Comunicador.iniciarLog("Iniciando treinamento da rede RBF");

    arquivoTreinamento.abrirArquivo();

    //Realiza primeiro o treinamento da camada escondida
    if (treinarCamadaEscondida(arquivoTreinamento) == false) {
      return false;
    }

    //Por ultimo realiza o treinamento da camada de saida
    if (treinarCamadaSaida(arquivoTreinamento) == false) {
      return false;
    }

    Comunicador.addLog("Rede RBF treinada com sucesso");
    imprimirPesosCamadaEscondida();
    imprimirPesosCamadaSaida();

    //Apenas se os 2 treinamentos forem bem sucedidos retorna verdadeiro
    return true;
  }

  public void testar(Arquivo arquivoTeste) {
    String log1;
    String log2;
    String log3;
    boolean errou;
    int numErros;

    Comunicador.iniciarLog("Iniciando teste da rede RBF");

    arquivoTeste.abrirArquivo();
    numErros = 0;

    //Para cada amostra do conjunto de teste
    for (String linha : arquivoTeste.getLinhasArquivo()) {
      recuperarEntradas(linha);

      //Calculando saida da rede
      calcularSaida();

      log1 = "";
      log2 = "";
      log3 = "";
      errou = false;
      for (int neuronio = 0; neuronio < NUM_NEU_CAMADA_SAIDA; neuronio++) {
        log1 += String.format("%f ", saidasEsperadas[neuronio]);
        log2 += String.format("%s ", Double.toString(saidaCamadaSaida[neuronio]));
        log3 += String.format("%f ", posProcessamento(saidaCamadaSaida[neuronio]));

        //Verifica se acertou ou errou a amostra
        if (posProcessamento(saidaCamadaSaida[neuronio]) != saidasEsperadas[neuronio]) {
          errou = true;
        }
      }

      Comunicador.addLog("" + log1 + log2 + log3 + "");

      if (errou) {
        numErros++;
      }
    }

    Comunicador.addLog("Fim do teste da RBF");
    Comunicador.addLog(String.format("Porcentagem de acerto: %.2f%%", (100D / (double) arquivoTeste.getLinhasArquivo().size()) * ((double) arquivoTeste.getLinhasArquivo().size() - numErros)));
  }

  private boolean treinarCamadaEscondida(Arquivo arquivoTreinamento) {
    List[] gruposOmega;
    double distanciaEuclidiana;
    double menorDistanciaEuclidiana;
    double valorParcial;
    int neuronioMenorDistanciaEuclidiana;
    int indiceAmostra;
    boolean mudouGrupoOmega;

    Comunicador.addLog("Treinamento camada escondida");

    //Iniciando o vetor que ira guardar os grupos omega
    gruposOmega = new List[NUM_NEU_CAMADA_ESCONDIDA];
    for (int i = 0; i < NUM_NEU_CAMADA_ESCONDIDA; i++) {
      gruposOmega[i] = new ArrayList();
    }

    try {
      //Iniciando pesos da camada escondida com os primeiros valores do conjunto de entradas
      for (int i = 0; i < NUM_NEU_CAMADA_ESCONDIDA; i++) {
        recuperarEntradas(arquivoTreinamento.getLinhaIndice(i));

        for (int j = 0; j < NUM_ENTRADAS; j++) {
          pesosCamadaEscondida[i][j] = entradas[j];
        }
      }

      do {
        mudouGrupoOmega = false;
        indiceAmostra = 0;

        //Para cada amostra de treinamento
        for (String linha : arquivoTreinamento.getLinhasArquivo()) {
          indiceAmostra++;
          recuperarEntradas(linha);
          menorDistanciaEuclidiana = Double.MAX_VALUE;
          neuronioMenorDistanciaEuclidiana = 0;

          //Calcula a distancia euclidoana da entrada para cada neuronio
          for (int neuronio = 0; neuronio < NUM_NEU_CAMADA_ESCONDIDA; neuronio++) {
            distanciaEuclidiana = 0D;

            for (int x = 0; x < NUM_ENTRADAS; x++) {
              distanciaEuclidiana += Math.pow(entradas[x] - pesosCamadaEscondida[neuronio][x], 2D);
            }

            distanciaEuclidiana = Math.sqrt(distanciaEuclidiana);

            //Verifica qual a menor distancia euclidiana
            if (distanciaEuclidiana < menorDistanciaEuclidiana) {
              menorDistanciaEuclidiana = distanciaEuclidiana;
              neuronioMenorDistanciaEuclidiana = neuronio;
            }
          }

          //Adiciona amostra no grupo omega do neuronio com menor distancia euclidiana
          if (adicionarNoGrupoOmega(indiceAmostra, neuronioMenorDistanciaEuclidiana, gruposOmega)) {
            mudouGrupoOmega = true;
          }
        }

        //Ajustando os pesos da camada escondida
        for (int neuronio = 0; neuronio < NUM_NEU_CAMADA_ESCONDIDA; neuronio++) {
          for (int peso = 0; peso < NUM_ENTRADAS; peso++) {
            valorParcial = 0D;

            for (int indice = 0; indice < gruposOmega[neuronio].size(); indice++) {
              recuperarEntradas(arquivoTreinamento.getLinhaNumero((int) gruposOmega[neuronio].get(indice)));

              valorParcial += entradas[peso];
            }

            if (!gruposOmega[neuronio].isEmpty()) {//Evita divisao por zero
              pesosCamadaEscondida[neuronio][peso] = (1D / (double) gruposOmega[neuronio].size()) * valorParcial;
            }
          }
        }
      } while (mudouGrupoOmega);

      //Calculando variancia de cada funcao de ativacao
      for (int neuronio = 0; neuronio < NUM_NEU_CAMADA_ESCONDIDA; neuronio++) {
        valorParcial = 0D;

        for (int amostra = 0; amostra < gruposOmega[neuronio].size(); amostra++) {
          recuperarEntradas(arquivoTreinamento.getLinhaNumero((int) gruposOmega[neuronio].get(amostra)));

          for (int i = 0; i < NUM_ENTRADAS; i++) {
            valorParcial += Math.pow(entradas[i] - pesosCamadaEscondida[neuronio][i], 2D);
          }
        }

        variancia[neuronio] = (1D / (double) gruposOmega[neuronio].size()) * valorParcial;
      }

      Comunicador.addLog("Fim do treinamento da camada escondida");
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  private boolean treinarCamadaSaida(Arquivo arquivoTreinamento) {
    Random random;
    List<double[]> listaZ;
    int numAmostra;
    double erroAtual;
    double erroAnterior;
    double gradiente;

    numEpocas = 0;
    numAmostra = 0;
    listaZ = new ArrayList<double[]>();

    //Iniciando pesos da camada de saida com valores aleatorios enttre 0 e 1
    random = new Random();

    for (int i = 0; i < NUM_NEU_CAMADA_SAIDA; i++) {
      for (int j = 0; j < NUM_NEU_CAMADA_ESCONDIDA + 1; j++) {
        pesosCamadaSaida[i][j] = random.nextDouble();
      }
    }

    //Calculando os valores dos vetores Z
    for (String linha : arquivoTreinamento.getLinhasArquivo()) {
      numAmostra++;

      //Calculando saida da camada escondida
      recuperarEntradas(linha);
      calcularSaida();
      listaZ.add(new double[NUM_NEU_CAMADA_ESCONDIDA]);
      
      for (int i = 0; i < NUM_NEU_CAMADA_ESCONDIDA; i++) {
        listaZ.get(numAmostra - 1)[i] = saidaCamadaEscondida[i];
      }
    }

    //Para cada amostra do conjunto de treinamento calcula o gradiente e atualiza os pesos da camada de saida
    erroAtual = erroQuadraticoMedio(arquivoTreinamento);
    do {
      erroAnterior = erroAtual;

      for (String linha : arquivoTreinamento.getLinhasArquivo()) {
        recuperarEntradas(linha);
        
        calcularSaida();

        //Atualizando pesos sinapticos da camada de saida em funcao do valor do gradiente
        for (int neuronio = 0; neuronio < NUM_NEU_CAMADA_SAIDA; neuronio++) {
          gradiente = saidasEsperadas[neuronio] - saidaCamadaSaida[neuronio];

          for (int peso = 0; peso < NUM_NEU_CAMADA_ESCONDIDA + 1; peso++) {
            pesosCamadaSaida[neuronio][peso] += TAXA_APRENDIZADO * gradiente * saidaCamadaEscondida[peso];
          }
        }
      }

      erroAtual = erroQuadraticoMedio(arquivoTreinamento);
      numEpocas++;
      Comunicador.addLog(String.format("%d %s", numEpocas, Double.toString(erroAtual)));
    } while (Math.abs(erroAtual - erroAnterior) > PRECISAO);

    return true;
  }

  private void calcularSaida() {
    double valorParcial;

    //Calculando saida da camada escondida
    saidaCamadaEscondida[0] = LIMIAR_ATIVACAO;
    for (int neuronio = 0; neuronio < NUM_NEU_CAMADA_ESCONDIDA; neuronio++) {
      valorParcial = 0D;

      for (int entrada = 0; entrada < NUM_ENTRADAS; entrada++) {
        valorParcial += Math.pow(entradas[entrada] - pesosCamadaEscondida[neuronio][entrada], 2D);
      }

      saidaCamadaEscondida[neuronio+1] = Math.pow(Math.E, (-1D) * (valorParcial / (2D * variancia[neuronio])));
    }

    //Calculando saida da camada de saida
    for (int neuronio = 0; neuronio < NUM_NEU_CAMADA_SAIDA; neuronio++) {
      valorParcial = 0D;

      for (int i = 0; i < NUM_NEU_CAMADA_ESCONDIDA + 1; i++) {
        valorParcial += pesosCamadaSaida[neuronio][i] * saidaCamadaEscondida[i];
      }

      saidaCamadaSaida[neuronio] = valorParcial;
    }

  }

  private void recuperarEntradas(String linha) {
    String[] vetor;
    int i;

    vetor = linha.split("\\s+");
    i = 0;

    if (vetor[0].equals("")) {
      i++;
    }

    //preenche o vetor de entradas a partir da linha lida do arquivo
    for (int j = 0; j < NUM_ENTRADAS; j++) {
      entradas[j] = Numero.parseDouble(vetor[i++]);
    }

    //preenche o vetor das saidas esperadas a partir da linha lida do arquivo
    if (vetor.length > i) {
      for (int j = 0; i + j < vetor.length; j++) {
        saidasEsperadas[j] = Numero.parseDouble(vetor[i++]);
      }
    }
  }

  private boolean adicionarNoGrupoOmega(int amostra, int neuronio, List[] gruposOmega) {
    //Se a amostra ja esta no grupo nao ha  mudanca
    for (int indice = 0; indice < gruposOmega[neuronio].size(); indice++) {
      if ((int) gruposOmega[neuronio].get(indice) == amostra) {
        return false;
      }
    }

    //Se a amostra esta em outro grupo deve ser removida
    for (int i = 0; i < NUM_NEU_CAMADA_ESCONDIDA; i++) {
      //Nao olha no grupo onde deve ser adicionado
      if (i == neuronio) {
        continue;
      }

      //remove a amostra do grupo antigo
      for (int indice = 0; indice < gruposOmega[i].size(); indice++) {
        if ((int) gruposOmega[i].get(indice) == amostra) {
          gruposOmega[i].remove(indice);
        }
      }
    }

    //Adiciona amostra no grupo certo
    gruposOmega[neuronio].add(amostra);

    return true;
  }

  private double erroQuadraticoMedio(Arquivo arquivoTreinamento) {
    double erro;
    double erroQuadMedio;
    int numAmostras;

    numAmostras = 0;
    erro = 0D;
    erroQuadMedio = 0D;

    //Para cada amostra do conjunto de treinamento
    for (String linha : arquivoTreinamento.getLinhasArquivo()) {
      numAmostras++;
      recuperarEntradas(linha);
      calcularSaida();
      erro = 0D;

      //O Erro e a soma dos erros de cada neuronio da camada de saida
      for (int neuronio = 0; neuronio < NUM_NEU_CAMADA_SAIDA; neuronio++) {
        erro += Math.pow(saidasEsperadas[neuronio] - saidaCamadaSaida[neuronio], 2D);
      }

      erro = erro / 2D;
      erroQuadMedio += erro;
    }

    //Calculando Eqm
    erroQuadMedio = erroQuadMedio / numAmostras;

    return erroQuadMedio;
  }

  //Imprime os pesos sinapticos da camada escondida
  private void imprimirPesosCamadaEscondida() {
    String texto;

    Comunicador.addLog("Pesos camada escondida:");

    for (int neuronio = 0; neuronio < NUM_NEU_CAMADA_ESCONDIDA; neuronio++) {
      texto = "";

      for (int i = 0; i < NUM_ENTRADAS; i++) {
        if (i > 0) {
          texto += ", ";
        }

        texto += String.format("%s", Double.toString(pesosCamadaEscondida[neuronio][i]));
      }

      Comunicador.addLog(String.format("  N%d: centro = (%s); variancia = %s", (neuronio + 1), texto, Double.toString(variancia[neuronio])));
    }
  }

  //Imprime os pesos sinapticos da camada de saida
  private void imprimirPesosCamadaSaida() {
    String texto;

    Comunicador.addLog("Pesos camada de saida");

    for (int neuronio = 0; neuronio < NUM_NEU_CAMADA_SAIDA; neuronio++) {
      texto = "";

      for (int i = 0; i < NUM_NEU_CAMADA_ESCONDIDA + 1; i++) {
        texto += String.format("%s ", Double.toString(pesosCamadaSaida[neuronio][i]));
      }

      Comunicador.addLog(String.format("N%d: %s", neuronio + 1, texto));
    }
  }

  //Realiza o pos processamento
  private double posProcessamento(double val) {
    if (val >= 0D) {
      return 1D;
    }

    return -1D;
  }

}

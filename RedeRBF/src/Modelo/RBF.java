package Modelo;

import Controle.Comunicador;
import Recursos.Arquivo;
import Recursos.Numero;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Jônatas Trabuco Belotti [jonatas.t.belotti@hotmail.com]
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
    this.pesosCamadaSaida = new double[NUM_NEU_CAMADA_SAIDA][NUM_NEU_CAMADA_ESCONDIDA + 1];//+1 por causa do peso do limiar de ativação
    this.saidaCamadaEscondida = new double[NUM_NEU_CAMADA_ESCONDIDA + 1];//+1 por causa do limiar de ativação
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

    //Por ultimo realiza o treinamento da camada de saída
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

    for (String linha : arquivoTeste.getLinhasArquivo()) {
      recuperarEntradas(linha);

      //Calculando saída da rede
      calcularSaida();

      log1 = "";
      log2 = "";
      log3 = "";
      errou = false;
      for (int neuronio = 0; neuronio < NUM_NEU_CAMADA_SAIDA; neuronio++) {
        log1 += String.format("%g ", saidasEsperadas[neuronio]);
        log2 += String.format("%g ", saidaCamadaSaida[neuronio]);
        log3 += String.format("%g ", posProcessamento(saidaCamadaSaida[neuronio]));

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

    //Iniciando o vetor que irá guardar os grupos omega
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

          //Calcula a distância euclidoana da entrada para cada neuronio
          for (int neuronio = 0; neuronio < NUM_NEU_CAMADA_ESCONDIDA; neuronio++) {
            distanciaEuclidiana = 0D;

            for (int x = 0; x < NUM_ENTRADAS; x++) {
              distanciaEuclidiana += Math.pow(entradas[x] - pesosCamadaEscondida[neuronio][x], 2D);
            }

            distanciaEuclidiana = Math.sqrt(distanciaEuclidiana);

            if (distanciaEuclidiana < menorDistanciaEuclidiana) {
              menorDistanciaEuclidiana = distanciaEuclidiana;
              neuronioMenorDistanciaEuclidiana = neuronio;
            }
          }

          if (adicionarNoGrupoOmega(indiceAmostra, neuronioMenorDistanciaEuclidiana, gruposOmega)) {
            mudouGrupoOmega = true;
          }
        }

        //Ajustando os pesos
        for (int neuronio = 0; neuronio < NUM_NEU_CAMADA_ESCONDIDA; neuronio++) {
          for (int peso = 0; peso < NUM_ENTRADAS; peso++) {
            valorParcial = 0D;

            for (int indice = 0; indice < gruposOmega[neuronio].size(); indice++) {
              recuperarEntradas(arquivoTreinamento.getLinhaNumero((int) gruposOmega[neuronio].get(indice)));

              valorParcial += entradas[peso];
            }

            if (!gruposOmega[neuronio].isEmpty()) {//Evita divisão por zero
              pesosCamadaEscondida[neuronio][peso] = (1D / (double) gruposOmega[neuronio].size()) * valorParcial;
            }
          }
        }
      } while (mudouGrupoOmega);

      //Calculando variancia de cada função de ativação
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

    //Iniciando pesos da camada de saída com valores aleatórios enttre 0 e 1
    random = new Random();

    for (int i = 0; i < NUM_NEU_CAMADA_SAIDA; i++) {
      for (int j = 0; j < NUM_NEU_CAMADA_ESCONDIDA + 1; j++) {
        pesosCamadaSaida[i][j] = random.nextDouble();
      }
    }

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

    //
    erroAtual = erroQuadraticoMedio(arquivoTreinamento);
    do {
      erroAnterior = erroAtual;

      for (String linha : arquivoTreinamento.getLinhasArquivo()) {
        recuperarEntradas(linha);
        
        calcularSaida();

        //Atualizando pesos sinapticos da camada de saída em função do valor do gradiente
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

    //Calculando saida da camada de saída
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

    for (int j = 0; j < NUM_ENTRADAS; j++) {
      entradas[j] = Numero.parseDouble(vetor[i++]);
    }

    if (vetor.length > i) {
      for (int j = 0; i + j < vetor.length; j++) {
        saidasEsperadas[j] = Numero.parseDouble(vetor[i++]);
      }
    }
  }

  private boolean adicionarNoGrupoOmega(int amostra, int neuronio, List[] gruposOmega) {
    //Se a amostra já está no grupo não há  mudança
    for (int indice = 0; indice < gruposOmega[neuronio].size(); indice++) {
      if ((int) gruposOmega[neuronio].get(indice) == amostra) {
        return false;
      }
    }

    //Se a amostra está em outro grupo deve ser removida
    for (int i = 0; i < NUM_NEU_CAMADA_ESCONDIDA; i++) {
      //Não olha no grupo onde deve ser adicionado
      if (i == neuronio) {
        continue;
      }

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

    for (String linha : arquivoTreinamento.getLinhasArquivo()) {
      numAmostras++;
      recuperarEntradas(linha);
      calcularSaida();
      erro = 0D;

      for (int neuronio = 0; neuronio < NUM_NEU_CAMADA_SAIDA; neuronio++) {
        erro += Math.pow(saidasEsperadas[neuronio] - saidaCamadaSaida[neuronio], 2D);
      }

      erro = erro / 2D;
      erroQuadMedio += erro;
    }

    erroQuadMedio = erroQuadMedio / numAmostras;

    return erroQuadMedio;
  }

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

  private double posProcessamento(double val) {
    if (val >= 0D) {
      return 1D;
    }

    return -1D;
  }

}

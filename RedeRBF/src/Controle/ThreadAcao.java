package Controle;

import Modelo.RBF;
import Recursos.Arquivo;
import javax.swing.JOptionPane;

/**
 *
 * @author Jônatas Trabuco Belotti [jonatas.t.belotti@hotmail.com]
 */
public class ThreadAcao extends Thread {

  private RBF redeRBF = null;
  private Arquivo arquivoTreinamento;

  public ThreadAcao(RBF redeRBF) {
    this.redeRBF = redeRBF;
  }

  public void setArquivoTreinamento(Arquivo arquivoTreinamento) {
    this.arquivoTreinamento = arquivoTreinamento;
  }

  @Override
  public void run() {
    if (redeRBF != null && arquivoTreinamento != null) {
      imprimirMensagem(redeRBF.treinar(arquivoTreinamento));

      stop();
    }
  }

  private void imprimirMensagem(boolean val) {
    if (val) {
      JOptionPane.showMessageDialog(null, "Rede neural treinada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
      Comunicador.setEnabledBotaoTestar(true);
      Comunicador.setEnabledBotaoSalvar(true);
    } else {
      JOptionPane.showMessageDialog(null, "Houve um erro no treinamento da rede!", "Erro", JOptionPane.ERROR_MESSAGE);
      Comunicador.setEnabledBotaoTestar(false);
      Comunicador.setEnabledBotaoSalvar(false);
    }
  }

}

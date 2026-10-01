package br.com.vistoriapredial.vistoria.application;

import br.com.vistoriapredial.vistoria.application.ia.SolicitacaoAnaliseIa;

public interface IaIntegrationService {

    String analisar(SolicitacaoAnaliseIa solicitacao);
}

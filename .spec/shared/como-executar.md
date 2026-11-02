# Como Executar

Estas instruções valem para a execução de qualquer especificação do projeto. O objetivo é manter o trabalho rastreável com evidências curtas, sem transformar a spec num relatório extenso.

## Antes de iniciar

1. Leia o objetivo, o contexto técnico, o escopo, as restrições e os critérios de aceitação da spec.
2. Consulte a memória do produto, o contexto técnico global e as instruções aplicáveis ao trabalho.
3. Confirme que a spec e a execução pretendida já foram autorizadas. Aprovação documental não autoriza automaticamente implementação ou migração. Aproveite autorizações já dadas para o mesmo escopo; não peça confirmação a cada tarefa.
4. Verifique o estado atual do projeto, as alterações existentes e os pré-requisitos relevantes. Preserve trabalho que não pertence à mudança.
5. Identifique dependências entre tarefas e questões que realmente impedem o próximo passo. Não invente requisitos para preencher lacunas.

## Regras de execução

1. Execute as tarefas na ordem apresentada dentro de cada grupo e respeite as dependências entre grupos.
2. Trabalhe apenas no escopo aprovado. Melhorias adicionais devem ser registadas como propostas, sem implementação automática.
3. Use a ferramenta ou automação indicada na tarefa — skill, script ou gerador — como abordagem principal quando disponível e aplicável. Leia as suas instruções antes de usar.
4. Se a ferramenta cobrir apenas parte do trabalho, use-a nessa parte e complete o restante dentro do escopo autorizado. Registe a limitação e a alternativa adotada.
5. Se a ferramenta estiver indisponível ou for incompatível, não improvise resultados. Registe o motivo; use uma alternativa equivalente quando ela preservar o escopo e a ferramenta não for um requisito obrigatório.
6. Execute a verificação adequada antes de concluir uma tarefa. Criar ou alterar um ficheiro, por si só, não comprova que o comportamento está correto.
7. Marque o checkbox somente quando a tarefa e a sua validação necessária estiverem concluídas. Adicione a evidência imediatamente abaixo.
8. Se a tarefa estiver parcial, bloqueada ou falhar, mantenha o checkbox desmarcado e registe o que falta.
9. Nunca remova uma tarefa nem apague evidências anteriores. Acrescente atualizações para preservar o histórico.
10. Se uma tarefa antes concluída deixar de satisfazer o resultado esperado, reabra o checkbox e registe a causa. Não mantenha uma conclusão que deixou de ser verdadeira.
11. Ao interromper o trabalho, registe o ponto de paragem e a próxima ação útil. Ao retomar, confira as evidências e o estado atual antes de repetir operações.

## Formato da evidência

Use uma linha curta logo abaixo da tarefa. Inclua data, hora e fuso, resultado verificável e desvios relevantes. Use o fuso configurado para o projeto ou, na ausência dele, o do utilizador.

### Tarefa concluída

```md
- [x] Descrição da tarefa.
  > ✅ YYYY-MM-DD HH:MM ±HH:MM — Resultado entregue. Verificação: comando ou revisão realizada e resultado. Desvio: apenas se houver.
```

### Tarefa parcial ou bloqueada

```md
- [ ] Descrição da tarefa.
  > ⏳ YYYY-MM-DD HH:MM ±HH:MM — Parcial: parte concluída. Falta: resultado ou validação pendente. Próxima ação: passo necessário.
  > ⛔ YYYY-MM-DD HH:MM ±HH:MM — Bloqueio: motivo concreto. Para desbloquear: informação, acesso ou dependência necessária.
```

Os dois registos acima ilustram estados possíveis; não é necessário usar ambos. Quando o bloqueio for resolvido, acrescente a evidência de conclusão e marque o checkbox, preservando os registos anteriores.

### O que conta como evidência

- **Código:** comportamento alterado e teste ou verificação pertinente, com o resultado observado.
- **Contrato:** alteração realizada e resultado da validação do contrato e dos consumidores afetados, quando aplicável.
- **Documento:** conteúdo criado ou revisto e conferência de consistência com as decisões vigentes.
- **Decisão:** escolha, motivo e referência à aprovação quando necessária.

Identifique ficheiros, comandos, testes ou artefactos apenas na medida necessária para localizar e reproduzir a verificação. Não cole logs completos, credenciais ou dados financeiros reais.

Não use “feito”, “testado” ou “funcionou” como única evidência. Distinga sempre teste executado, teste apenas escrito e verificação não executada. Não invente horários, resultados ou aprovações.

## Bloqueios e alterações do plano

- Um bloqueio afeta a tarefa e as suas dependentes. Continue tarefas independentes já autorizadas quando isso respeitar a ordem e as dependências previstas.
- Uma escolha técnica local que mantém comportamento, contrato e escopo pode ser resolvida durante a execução e registada na evidência.
- Mudanças de comportamento, contrato, escopo ou decisão arquitetural aprovada precisam de revisão e autorização antes da execução da parte afetada.
- Dividir ou detalhar uma tarefa é permitido para tornar o trabalho executável, preservando a tarefa original e a rastreabilidade.
- Se uma tarefa deixar de fazer sentido, não a marque como concluída artificialmente. Registe a proposta de cancelamento ou substituição e obtenha aprovação quando ela alterar o plano aprovado.
- Uma tarefa cancelada permanece visível e desmarcada, identificada como **cancelada**, com motivo e aprovação. Se houver substituição, indique a nova tarefa. Cancelamento não equivale a execução bem-sucedida.
- Se houver conflito entre documentos, registe a divergência e resolva apenas o necessário para avançar. Uma instrução explícita do utilizador pode atualizar o plano, sem implicar aprovação de mudanças adicionais.

## Quando a spec está concluída

Considere a spec concluída somente quando:

- Todas as tarefas ativas estiverem marcadas e acompanhadas de evidência.
- Os critérios de aceitação estiverem atendidos e verificados.
- As verificações obrigatórias tiverem sido executadas com resultado satisfatório.
- Não houver bloqueios ou falhas relevantes em aberto.
- Desvios e cancelamentos que exigiam aprovação estiverem aprovados e registados.
- A documentação afetada estiver coerente com o resultado entregue.

Tarefas canceladas com aprovação não contam como pendências, mas permanecem no histórico. Uma verificação obrigatória não executada mantém a tarefa pendente, salvo revisão explícita do critério aprovado.

Ao concluir, acrescente um resumo curto do resultado, das validações realizadas e de eventuais limitações aceites. Não copie todas as evidências para o resumo.

## Intenção

Manter uma ligação clara entre o que foi pedido, o que foi feito e como foi verificado. A evidência deve ser suficiente para retomar ou rever o trabalho, sem repetir a especificação inteira.

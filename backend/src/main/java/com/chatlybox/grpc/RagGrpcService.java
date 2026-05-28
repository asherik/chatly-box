package com.chatlybox.grpc;

import com.chatlybox.rag.RagFacade;
import com.chatlybox.search.DocumentSearchService;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class RagGrpcService extends RagServiceGrpc.RagServiceImplBase {
  private final RagFacade ragFacade;
  private final DocumentSearchService searchService;

  public RagGrpcService(RagFacade ragFacade, DocumentSearchService searchService) {
    this.ragFacade = ragFacade;
    this.searchService = searchService;
  }

  @Override
  public void ask(AskRequest request, StreamObserver<AskResponse> responseObserver) {
    responseObserver.onNext(AskResponse.newBuilder().setAnswer(ragFacade.ask(request.getQuestion())).build());
    responseObserver.onCompleted();
  }

  @Override
  public void searchDocuments(SearchRequest request, StreamObserver<SearchResponse> responseObserver) {
    SearchResponse.Builder response = SearchResponse.newBuilder();
    searchService.search(request.getQuery(), request.getLimit() <= 0 ? 10 : request.getLimit()).forEach(hit ->
        response.addHits(SearchHit.newBuilder()
            .setDocumentId(hit.documentId())
            .setTitle(hit.title())
            .setUri(hit.uri())
            .setExcerpt(hit.excerpt())
            .build()));
    responseObserver.onNext(response.build());
    responseObserver.onCompleted();
  }
}

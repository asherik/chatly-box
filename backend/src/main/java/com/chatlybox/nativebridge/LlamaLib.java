package com.chatlybox.nativebridge;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LlamaLib {
  private final MethodHandle ragQuery;
  private final MethodHandle indexDocument;
  private final MethodHandle freeString;
  private final boolean nativeLoaded;

  public LlamaLib(@Value("${chatly.native-library-path}") String libraryPath) {
    MethodHandle nextRagQuery = null;
    MethodHandle nextIndexDocument = null;
    MethodHandle nextFreeString = null;
    boolean loaded = false;
    try {
      System.load(libraryPath);
      Linker linker = Linker.nativeLinker();
      SymbolLookup lookup = SymbolLookup.loaderLookup();
      nextRagQuery = downcall(linker, lookup, "chatly_rag_query");
      nextIndexDocument = downcall(linker, lookup, "chatly_index_document");
      nextFreeString = downcall(linker, lookup, "chatly_free_string", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
      loaded = true;
    } catch (UnsatisfiedLinkError | IllegalStateException error) {
      loaded = false;
    }
    this.ragQuery = nextRagQuery;
    this.indexDocument = nextIndexDocument;
    this.freeString = nextFreeString;
    this.nativeLoaded = loaded;
  }

  public String ask(String requestJson) {
    if (!nativeLoaded) {
      return "llamalib native library is not loaded yet. Build llamalib with `cargo build --release` and set CHATLY_NATIVE_LIBRARY.";
    }
    return callString(ragQuery, requestJson);
  }

  public String index(String requestJson) {
    if (!nativeLoaded) {
      return "{\"status\":\"pending_native_library\"}";
    }
    return callString(indexDocument, requestJson);
  }

  private String callString(MethodHandle handle, String json) {
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment input = arena.allocateFrom(json, StandardCharsets.UTF_8);
      MemorySegment output = (MemorySegment) handle.invoke(input);
      String result = output.reinterpret(Long.MAX_VALUE).getString(0, StandardCharsets.UTF_8);
      if (freeString != null) {
        freeString.invoke(output);
      }
      return result;
    } catch (Throwable error) {
      throw new IllegalStateException("Native llamalib call failed", error);
    }
  }

  private static MethodHandle downcall(Linker linker, SymbolLookup lookup, String name) {
    return downcall(linker, lookup, name, FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
  }

  private static MethodHandle downcall(Linker linker, SymbolLookup lookup, String name, FunctionDescriptor descriptor) {
    return linker.downcallHandle(
        lookup.find(name).orElseThrow(() -> new IllegalStateException("Missing native symbol: " + name)),
        descriptor);
  }
}

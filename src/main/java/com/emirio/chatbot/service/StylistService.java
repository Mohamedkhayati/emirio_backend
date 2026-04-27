package com.emirio.chatbot.service;

import com.emirio.catalog.Article;
import com.emirio.catalog.repo.ArticleRepository;
import com.emirio.chatbot.shop.PdfShopInfoLoader;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StylistService {

    private final ChatClient chatClient;
    private final ArticleRepository articleRepository;
    private final PdfShopInfoLoader pdfLoader; // for fallback contact info

    public StylistService(ChatClient.Builder chatClientBuilder,
                          ArticleRepository articleRepository,
                          PdfShopInfoLoader pdfLoader) {
        this.chatClient = chatClientBuilder.build();
        this.articleRepository = articleRepository;
        this.pdfLoader = pdfLoader;
    }

    // Warm up the model on startup to avoid first‑request timeout
    @PostConstruct
    public void warmUpModel() {
        try {
            System.out.println("🔄 Warming up AI model (gemma3:1b)...");
            chatClient.prompt()
                .system("You are a shoe expert. Reply with 'ready'.")
                .user("Say ready")
                .call()
                .content();
            System.out.println("✅ AI model is ready.");
        } catch (Exception e) {
            System.out.println("⚠️ Could not warm up model: " + e.getMessage());
            System.out.println("Make sure Ollama is running on http://localhost:11434");
        }
    }

    public String getStylingAdvice(String userPrompt) {
        try {
            // Fetch active articles from database
            List<Article> articles = articleRepository.findByActifTrueOrderByIdDesc();
            String productList = articles.stream()
                    .limit(30) // keep prompt size reasonable
                    .map(a -> String.format("- %s : %.2f DT (catégorie: %s)",
                            a.getNom(), a.getPrix(),
                            a.getCategorie() != null ? a.getCategorie().getNom() : "Non catégorisé"))
                    .collect(Collectors.joining("\n"));

            String systemPrompt = """
                    Tu es un conseiller en style et en chaussures de la boutique "Emirio Chaussures".
                    Tu réponds toujours en français, de manière chaleureuse et professionnelle.
                    Tu donnes des conseils sincères sur les tenues et les chaussures, en utilisant uniquement le catalogue ci-dessous.
                    Si tu ne peux pas recommander un produit précis, suggère un type de chaussure (ex: "baskets blanches").
                    
                    Catalogue Emirio:
                    %s
                    
                    Important : ne mentionne jamais que tu es une IA. Parle comme un vendeur passionné.
                    """.formatted(productList);

            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

        } catch (Exception e) {
            // Fallback when AI is offline or times out
            String phone = pdfLoader.getShopDetails().getPhone();
            return "🌟 Notre conseiller styliste est en pleine préparation. " +
                   "En attendant, découvrez notre catalogue ou contactez-nous directement au " +
                   phone + " ! Nous sommes ravis de vous aider.";
        }
    }
}
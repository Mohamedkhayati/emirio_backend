package com.emirio.chatbot.service;

import com.emirio.catalog.Article;
import com.emirio.catalog.VariationArticle;
import com.emirio.catalog.repo.ArticleRepository;
import com.emirio.catalog.repo.VariationRepository;
import com.emirio.chatbot.entity.ChatMessage;
import com.emirio.chatbot.repository.ChatMessageRepository;
import com.emirio.chatbot.shop.PdfShopInfoLoader;
import com.emirio.chatbot.shop.ShopDetails;
import com.emirio.user.User;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final PdfShopInfoLoader pdfLoader;
    private final ArticleRepository articleRepository;
    private final VariationRepository variationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final EntityManager entityManager;

    private static final double FUZZY_THRESHOLD = 0.55;
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "avez-vous", "as-tu", "est-ce", "que", "y", "a-t-il", "il", "y", "a",
        "le", "la", "les", "un", "une", "des", "du", "de", "et", "ou", "donc", "or", "ni", "car",
        "en", "dans", "par", "pour", "sur", "sous", "avec", "sans", "chez", "entre", "parmi",
        "ce", "cet", "cette", "ces", "mon", "ton", "son", "notre", "votre", "leur",
        "quel", "quelle", "quels", "quelles", "combien", "comment", "pourquoi", "où", "quand",
        "est", "sont", "être", "avoir", "faire", "peut", "peuvent", "doit", "doivent", "être",
        "disponible", "quantité", "prix", "catalogue", "couleur", "colour", "couleurs", "colors"
    ));

    @Transactional
    public String processQuestion(User user, String question) {
        String normalized = question.toLowerCase().trim();
        ChatbotIntent intent = detectIntent(normalized);
        String answer = generateAnswer(intent, normalized, question);
        chatMessageRepository.save(new ChatMessage(user, question, answer));
        return answer;
    }

    private ChatbotIntent detectIntent(String normalized) {
        // 1. Greeting
        if (matchesAny(normalized, "bonjour", "salut", "hello", "hey", "coucou"))
            return ChatbotIntent.GREETING;
        // 2. Shop info
        if (matchesAny(normalized, "adresse", "où se trouve", "boutique", "magasin", "localisation", "emplacement"))
            return ChatbotIntent.SHOP_INFO;
        // 3. Contact
        if (matchesAny(normalized, "téléphone", "whatsapp", "email", "contact", "facebook", "instagram"))
            return ChatbotIntent.CONTACT;
        // 4. Best sellers
        if (matchesAny(normalized, "meilleur", "plus vendu", "top vente", "best seller", "plus populaire", "le plus vendu"))
            return ChatbotIntent.BEST_SELLERS;
        // 5. Sale items
        if (matchesAny(normalized, "promotion", "soldé", "sale", "réduction", "prix réduit", "promo"))
            return ChatbotIntent.SALE_ITEMS;
        // 6. Color query (must come before STOCK_QUERY because both use "disponible")
        if (matchesAny(normalized, "couleur", "colour", "couleurs", "colors", "quelles couleurs", "les couleurs"))
            return ChatbotIntent.COLOR_QUERY;
        // 7. Count articles
        if (matchesAny(normalized, "combien d'articles", "nombre d'articles", "total produits", "combien de produits"))
            return ChatbotIntent.COUNT_ARTICLES;
        // 8. List all articles in stock
        if (matchesAny(normalized, "tous les articles", "catalogue complet", "liste produits", "articles disponibles", "tout le catalogue", "liste tous les articles"))
            return ChatbotIntent.ARTICLES_IN_STOCK;
        // 9. Stock query (generic)
        if (matchesAny(normalized, "stock", "disponible", "quantité", "restant", "en stock"))
            return ChatbotIntent.STOCK_QUERY;
        // 10. Default: article search
        return ChatbotIntent.ARTICLE_SEARCH;
    }

    private boolean matchesAny(String text, String... keywords) {
        return Arrays.stream(keywords).anyMatch(text::contains);
    }

    private String generateAnswer(ChatbotIntent intent, String normalized, String originalQuestion) {
        switch (intent) {
            case GREETING:
                return "Bonjour ! Je suis l'assistant d'Emirio Chaussures. Posez-moi vos questions sur les produits, les stocks, nos boutiques ou les meilleures ventes.";
            case SHOP_INFO:
                ShopDetails shop = pdfLoader.getShopDetails();
                return String.format("🏬 %s\n📅 Fondée en %s par %s.\n👟 Spécialité : %s\n📍 Adresses :\n%s",
                        shop.getShopName(), shop.getFounded(), shop.getFounder(),
                        shop.getSpeciality(), shop.getAddressDetails());
            case CONTACT:
                ShopDetails contact = pdfLoader.getShopDetails();
                return String.format("📞 Téléphone / WhatsApp : %s\n📧 Email : %s\n🌐 Facebook : %s",
                        contact.getPhone(), contact.getEmail(), contact.getFacebook());
            case STOCK_QUERY:
                return checkStock(originalQuestion);
            case BEST_SELLERS:
                return getBestSellers();
            case SALE_ITEMS:
                return getSaleItems();
            case ARTICLES_IN_STOCK:
                return listAllInStock();
            case COUNT_ARTICLES:
                return getTotalArticleCount();
            case COLOR_QUERY:
                return getAvailableColors(originalQuestion);
            case ARTICLE_SEARCH:
            default:
                return searchArticles(originalQuestion);
        }
    }

    // ---------- PRODUCT NAME EXTRACTION (improved for color queries) ----------
    private String extractProductName(String rawQuestion, boolean isColorQuery) {
        String lower = rawQuestion.toLowerCase();
        // Remove common trigger words for color queries
        if (isColorQuery) {
            lower = lower.replaceAll("\\b(couleur|colour|couleurs|colors|disponible|quelles|les|des|pour|de|la|le|en)\\b", " ");
        }
        String[] words = lower.split("[\\s?;:!,.]+");
        List<String> filtered = new ArrayList<>();
        for (String w : words) {
            if (w.length() < 2) continue;
            if (STOP_WORDS.contains(w)) continue;
            filtered.add(w);
        }
        if (filtered.isEmpty()) {
            if (words.length >= 2) return words[words.length-2] + " " + words[words.length-1];
            if (words.length == 1) return words[0];
            return "";
        }
        return String.join(" ", filtered);
    }

    // Overload for non-color queries
    private String extractProductName(String rawQuestion) {
        return extractProductName(rawQuestion, false);
    }

    // ---------- SEARCH ARTICLES ----------
    private String searchArticles(String query) {
        String productName = extractProductName(query);
        if (productName.isEmpty()) {
            return "Veuillez préciser le nom de l'article que vous cherchez (ex: Basket Nike, Bottine Chelsea).";
        }

        List<Article> allArticles = articleRepository.findByActifTrueOrderByIdDesc();
        if (allArticles.isEmpty()) return "Aucun article dans le catalogue.";

        List<Article> matched = allArticles.stream()
                .filter(a -> a.getNom().toLowerCase().contains(productName))
                .collect(Collectors.toList());

        if (matched.isEmpty()) {
            LevenshteinDistance ld = new LevenshteinDistance();
            matched = allArticles.stream()
                    .filter(a -> {
                        String name = a.getNom().toLowerCase();
                        int maxLen = Math.max(name.length(), productName.length());
                        if (maxLen == 0) return false;
                        double sim = 1.0 - ((double) ld.apply(name, productName) / maxLen);
                        return sim >= FUZZY_THRESHOLD;
                    })
                    .limit(5)
                    .collect(Collectors.toList());
        }

        if (matched.isEmpty()) {
            String[] words = productName.split("\\s+");
            for (String w : words) {
                if (w.length() < 3) continue;
                List<Article> partial = allArticles.stream()
                        .filter(a -> a.getNom().toLowerCase().contains(w))
                        .limit(3)
                        .collect(Collectors.toList());
                if (!partial.isEmpty()) {
                    matched = partial;
                    break;
                }
            }
        }

        if (matched.isEmpty()) {
            return "Je n'ai pas trouvé d'article correspondant à \"" + productName + "\". Essayez un autre nom (ex: Bottine, Nike, Chelsea).";
        }

        StringBuilder sb = new StringBuilder("🔍 Voici les articles correspondants :\n");
        for (Article a : matched) {
            int stock = getTotalStockForArticle(a.getId());
            sb.append(String.format("• %s - %.2f DT (%s)\n", a.getNom(), a.getPrix(),
                    stock > 0 ? "en stock" : "rupture"));
        }
        return sb.toString();
    }

    // ---------- CHECK STOCK ----------
    private String checkStock(String query) {
        String productName = extractProductName(query);
        if (productName.isEmpty()) {
            return "Quel article souhaitez-vous vérifier ? (ex: Bottine Chelsea, Nike Air Max)";
        }

        List<Article> allArticles = articleRepository.findByActifTrueOrderByIdDesc();
        Optional<Article> matched = allArticles.stream()
                .filter(a -> a.getNom().toLowerCase().contains(productName))
                .findFirst();

        if (matched.isEmpty()) {
            return "Je n'ai pas trouvé l'article \"" + productName + "\". Vérifiez le nom ou parcourez notre catalogue.";
        }

        Article article = matched.get();
        int stock = getTotalStockForArticle(article.getId());
        String stockMsg = stock > 0 ? stock + " unités disponibles" : "actuellement en rupture de stock";
        return String.format("📦 %s : %s. Prix : %.2f DT.", article.getNom(), stockMsg, article.getPrix());
    }

    private int getTotalStockForArticle(Long articleId) {
        List<VariationArticle> variations = variationRepository.findByArticleId(articleId);
        return variations.stream().mapToInt(VariationArticle::getQuantiteStock).sum();
    }

    // ---------- BEST SELLERS ----------
    private String getBestSellers() {
        String sql = """
            SELECT a.id, a.nom, SUM(lc.quantite) as total_sold
            FROM ligne_commande lc
            JOIN variation_article v ON lc.variation_id = v.id
            JOIN article a ON v.article_id = a.id
            GROUP BY a.id, a.nom
            ORDER BY total_sold DESC
            LIMIT 5
        """;
        try {
            Query query = entityManager.createNativeQuery(sql);
            List<Object[]> results = query.getResultList();
            if (results.isEmpty()) {
                return "Aucune donnée de vente disponible pour le moment.";
            }
            StringBuilder sb = new StringBuilder("🏆 Top 5 des articles les plus vendus :\n");
            int rank = 1;
            for (Object[] row : results) {
                String articleName = (String) row[1];
                Number sold = (Number) row[2];
                sb.append(String.format("%d. %s - %d unités vendues\n", rank++, articleName, sold.intValue()));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Fonctionnalité meilleures ventes en cours d'activation.";
        }
    }

    // ---------- SALE ITEMS ----------
    private String getSaleItems() {
        List<Article> articles = articleRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        List<Article> onSale = articles.stream()
                .filter(a -> a.getSalePrice() != null && a.getSaleStartAt() != null && a.getSaleEndAt() != null &&
                        !now.isBefore(a.getSaleStartAt()) && !now.isAfter(a.getSaleEndAt()))
                .collect(Collectors.toList());
        if (onSale.isEmpty()) return "Il n'y a actuellement aucune promotion.";
        StringBuilder sb = new StringBuilder("🔥 Articles en promotion :\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Article a : onSale) {
            sb.append(String.format("• %s : %.2f DT au lieu de %.2f DT (jusqu'au %s)\n",
                    a.getNom(), a.getSalePrice(), a.getPrix(),
                    a.getSaleEndAt().format(formatter)));
        }
        return sb.toString();
    }

    // ---------- LIST ALL IN STOCK ----------
    private String listAllInStock() {
        List<VariationArticle> variations = variationRepository.findAll();
        Set<Article> articlesWithStock = variations.stream()
                .filter(v -> v.getQuantiteStock() > 0)
                .map(VariationArticle::getArticle)
                .collect(Collectors.toSet());
        if (articlesWithStock.isEmpty()) return "Aucun article en stock.";
        StringBuilder sb = new StringBuilder("📋 Articles disponibles :\n");
        for (Article a : articlesWithStock) {
            int stock = getTotalStockForArticle(a.getId());
            sb.append(String.format("• %s - %d unités (%.2f DT)\n", a.getNom(), stock, a.getPrix()));
        }
        return sb.toString();
    }

    // ---------- TOTAL ARTICLE COUNT ----------
    private String getTotalArticleCount() {
        long total = articleRepository.count();
        long active = articleRepository.findByActifTrueOrderByIdDesc().size();
        return String.format("📊 Notre catalogue contient %d articles au total, dont %d actifs.", total, active);
    }

    // ---------- AVAILABLE COLORS FOR AN ARTICLE ----------
    private String getAvailableColors(String query) {
        // Extract product name: remove common color-query words and misspellings
        String productName = extractProductNameForColor(query);
        if (productName.isEmpty()) {
            return "Pour quel article voulez-vous connaître les couleurs ? (ex: Basketball Pro Homme)";
        }

        List<Article> allArticles = articleRepository.findByActifTrueOrderByIdDesc();
        // Try exact substring match
        Optional<Article> articleOpt = allArticles.stream()
                .filter(a -> a.getNom().toLowerCase().contains(productName))
                .findFirst();
        
        // If not found, try fuzzy on the last 2 words (assuming article name at the end)
        if (articleOpt.isEmpty()) {
            String[] words = productName.split("\\s+");
            for (int i = words.length; i > 0; i--) {
                String candidate = String.join(" ", Arrays.copyOfRange(words, i-1, words.length));
                if (candidate.length() < 3) continue;
                String finalCandidate = candidate;
                articleOpt = allArticles.stream()
                        .filter(a -> a.getNom().toLowerCase().contains(finalCandidate))
                        .findFirst();
                if (articleOpt.isPresent()) break;
            }
        }
        
        // If still not found, try fuzzy matching (Levenshtein) on all articles
        if (articleOpt.isEmpty()) {
            LevenshteinDistance ld = new LevenshteinDistance();
            articleOpt = allArticles.stream()
                    .min((a1, a2) -> {
                        double sim1 = 1.0 - ((double) ld.apply(a1.getNom().toLowerCase(), productName) / Math.max(a1.getNom().length(), productName.length()));
                        double sim2 = 1.0 - ((double) ld.apply(a2.getNom().toLowerCase(), productName) / Math.max(a2.getNom().length(), productName.length()));
                        return Double.compare(sim2, sim1);
                    });
            if (articleOpt.isPresent()) {
                double similarity = 1.0 - ((double) ld.apply(articleOpt.get().getNom().toLowerCase(), productName) / Math.max(articleOpt.get().getNom().length(), productName.length()));
                if (similarity < 0.5) articleOpt = Optional.empty();
            }
        }
        
        if (articleOpt.isEmpty()) {
            return "Je n'ai pas trouvé l'article correspondant à \"" + productName + "\". Vérifiez le nom.";
        }
        
        Article article = articleOpt.get();
        List<VariationArticle> variations = variationRepository.findByArticleId(article.getId());
        Set<String> colors = variations.stream()
                .map(v -> v.getCouleur().getNom())
                .collect(Collectors.toSet());
        
        if (colors.isEmpty()) {
            return "Aucune couleur disponible pour " + article.getNom();
        }
        
        String colorList = String.join(", ", colors);
        return String.format("🎨 L'article **%s** est disponible dans les couleurs suivantes : %s.", article.getNom(), colorList);
    }

    private String extractProductNameForColor(String rawQuestion) {
        String lower = rawQuestion.toLowerCase();
        // Remove all words that are color-query related, including misspellings
        lower = lower.replaceAll("\\b(couleur|colour|couleurs|colors|disponible|dsiponible|dispon|quelles|les|des|pour|de|la|le|en|un|une|du|au|aux)\\b", " ");
        // Also remove any word that contains "dispon" (to catch misspellings)
        lower = lower.replaceAll("\\b\\w*dispon\\w*\\b", " ");
        // Remove extra spaces
        lower = lower.trim().replaceAll("\\s+", " ");
        // Now take all remaining words as product name
        if (lower.isEmpty()) {
            // fallback: take last 2-3 words from original query
            String[] words = rawQuestion.toLowerCase().split("\\s+");
            if (words.length >= 3) return words[words.length-3] + " " + words[words.length-2] + " " + words[words.length-1];
            if (words.length >= 2) return words[words.length-2] + " " + words[words.length-1];
            if (words.length >= 1) return words[words.length-1];
            return "";
        }
        return lower;
    }
}
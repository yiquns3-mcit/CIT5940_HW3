import java.util.*;
import java.io.*;

public class BookRecommender {

    public static void main(String[] args){
        try {
            if (args.length < 2) {
                System.out.println("Invalid input: Please provide csv_file and command.");
                return;
            }

            String csvFile = args[0];
            String command = args[1];

            // parse the CSV file (user to books graph)
            Map<String, Set<String>> userToBooks = parseCSV(csvFile);
            // build the graph (book to book graph)
            Map<String, Map<String, Integer>> bookToBookGraph = buildGraph(userToBooks);
            // build the graph (book to user graph)
            Map<String, Set<String>> bookToUsers = buildBookToUsers(userToBooks);
            // get the recommendation based on the command
            if (command.equals("single_book_mn")) {
                if (args.length != 3) return;
                String bookId = args[2];
                String result = singleBookRecommend(bookToBookGraph, bookId);
                System.out.println(result);
            } else if (command.equals("like_history_mn")) {
                if (args.length < 3) return;
                String[] inputBooks = Arrays.copyOfRange(args, 2, args.length);
                String result = likeHistoryRecommend(bookToBookGraph, inputBooks);
                System.out.println(result);
            } else if (command.equals("user_cf")) {
                if (args.length != 3) return;
                String targetUser = args[2];
                String result = userCF(userToBooks, bookToUsers, targetUser);
                System.out.println(result);
            } else if (command.equals("shortest_path")) {
                if (args.length != 4) return;
                String sourceBook = args[2];
                String targetBook = args[3];
                String result = shortestPath(bookToBookGraph, sourceBook, targetBook);
                System.out.println(result);
            } else {
                System.out.println("Invalid command");
            }
        } catch (Exception e) {
            System.out.println("NONE");
        }
    }

    // Part 0: Parsing the CSV file
    // example: userToBooks = {(node)user1: {(edge)book1, (edge)book2, ...}, ...}
    // * user based graph (later will create a book based graph in Part 3)
    public static Map<String, Set<String>> parseCSV(String csvFile) throws Exception {
        Map<String, Set<String>> userToBooks = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(csvFile));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            int comma = line.indexOf(',');
            if (comma < 0) {
                continue;
            }
            String user = line.substring(0, comma).trim();
            String book = line.substring(comma + 1).trim();
            userToBooks.putIfAbsent(user, new HashSet<>());
            userToBooks.get(user).add(book);
        }
        br.close();
        return userToBooks;
    }

    // Part 1: Creating your initial graph (use adjacency list structure)
    // example: map = {(node)book1: {(edge)book2: (weight)1, (edge)book3: (weight)2}, ...}
    public static Map<String, Map<String, Integer>> buildGraph(Map<String, Set<String>> bookMap) {
        Map<String, Map<String, Integer>> graph = new HashMap<>();
        // put all books into the graph
        for (String user : bookMap.keySet()) {
            for (String book : bookMap.get(user)) {
                graph.putIfAbsent(book, new HashMap<>());
            }
        }
        // set the edges and the weights of each book
        for (String user : bookMap.keySet()) {
            List<String> books = new ArrayList<>(bookMap.get(user));
            // iterate through all books (pair by pair)
            for (int i = 0; i < books.size() - 1; i++) {
                for (int j = i + 1; j < books.size(); j++) {
                    // get the book names (in this pair)
                    String book1 = books.get(i);
                    String book2 = books.get(j);
                    // update the edge and the weight of each book
                    graph.get(book1).put(book2, graph.get(book1).getOrDefault(book2, 0) + 1);
                    graph.get(book2).put(book1, graph.get(book2).getOrDefault(book1, 0) + 1);                    
                }
            }
        }
        return graph;
    }

    // Part 2: Item-based Collaborative Filtering
    // Part 2a: Single-Book Nearest Neighbors
    public static String singleBookRecommend(Map<String, Map<String, Integer>> graph, String book) {
        // no such book in the graph
        if (!graph.containsKey(book)) {
            return "NONE";
        }

        Map<String, Integer> neighbors = graph.get(book);

        // no neighbors of the book
        if (neighbors == null || neighbors.isEmpty()) {
            return "NONE";
        }

        List<Map.Entry<String,Integer>> books = new ArrayList<>(neighbors.entrySet());
        // sort the books:
        // 1. descending by weight
        // 2. alphabetical
        Collections.sort(books, (a, b) -> {
            int cmp = b.getValue() - a.getValue();
            if (cmp != 0) return cmp;
            return a.getKey().compareTo(b.getKey());
        });

        List<String> result = new ArrayList<>();

        // return at most 5 books
        int limit = Math.min(5, books.size());
        for (int i = 0; i < limit; i++) {
            result.add(books.get(i).getKey());
        }
        // if there are no recommended books, return NONE/
        if (result.isEmpty()) return "NONE";

        // return in a string format
        return String.join(",", result);
    }

    // Part 2b: Like-History Nearest Neighbors
    public static String likeHistoryRecommend(Map<String, Map<String, Integer>> bookMap, String[] inputBooks) {
        // compute the total weight of each candidate book
        Map<String, Integer> totalWeight = new HashMap<>();
        Set<String> inputSet = new HashSet<>(Arrays.asList(inputBooks));

        for (String book : inputSet) {
            if (!bookMap.containsKey(book)) {
                continue;
            }
            Map<String, Integer> neighbors = bookMap.get(book);
            if (neighbors == null) {
                continue;
            }
            for (Map.Entry<String, Integer> entry : neighbors.entrySet()) {
                String neighbor = entry.getKey();
                int weight = entry.getValue();
                if (inputSet.contains(neighbor)) {
                    continue;
                }
                totalWeight.put(neighbor, totalWeight.getOrDefault(neighbor, 0) + weight);
            }
        }

        if (totalWeight.isEmpty()) {
            return "NONE";
        }

        List<String> books = new ArrayList<>(totalWeight.keySet());
        Collections.sort(books, (a, b) -> {
            int weightA = totalWeight.get(a);
            int weightB = totalWeight.get(b);
            if (weightA != weightB) {
                return Integer.compare(weightB, weightA); // descending
            }
            return a.compareTo(b); // alphabetical
        });

        int limit = Math.min(5, books.size());
        return String.join(",", books.subList(0, limit));
    }

    // Part 3: Creating a User-Based Graph
    // example: bookToUsers = {(node)book1: {(edge)user1, (edge)user2, ...}, ...}
    // * book to user graph (since we already have user to book graph in Part 0)
    public static Map<String, Set<String>> buildBookToUsers(Map<String, Set<String>> userToBooks) {
        Map<String, Set<String>> bookToUsers = new HashMap<>();
        for (String user : userToBooks.keySet()) {
            for (String book : userToBooks.get(user)) {
                bookToUsers.putIfAbsent(book, new HashSet<>());
                bookToUsers.get(book).add(user);
            }
        }
        return bookToUsers;
    }

    // Part 4: User-based Collaborative Filtering
    public static String userCF(
        Map<String, Set<String>> userToBooks,
        Map<String, Set<String>> bookToUsers,
        String targetUser) {

        if (!userToBooks.containsKey(targetUser)) {
            return "NONE";
        }
        // Get the books liked by the target user
        Set<String> targetBooks = userToBooks.get(targetUser);
        if (targetBooks == null || targetBooks.isEmpty()) {
            return "NONE";
        }

        // 1. Compute Jaccard Similarity (b/t target user and other users)
        Map<String, Double> similarity = new HashMap<>();
        for (String otherUser : userToBooks.keySet()) {
            if (otherUser.equals(targetUser)) continue;
            // Get the books liked by the chosen other user
            Set<String> otherBooks = userToBooks.get(otherUser);
            // |A ∩ B|: number of books liked by both target user and other user
            Set<String> intersection = new HashSet<>(targetBooks);
            intersection.retainAll(otherBooks);
            if (intersection.isEmpty()) continue; // if no common books, skip
            // |A ∪ B|: number of books liked by either target user or other user
            Set<String> union = new HashSet<>(targetBooks);
            union.addAll(otherBooks);
            // Compute the Jaccard Similarity: |A ∩ B| / |A ∪ B|
            double jaccardScore = (double) intersection.size() / union.size();
            similarity.put(otherUser, jaccardScore);
        }
        // If there are no similarity scores, return NONE
        if (similarity.isEmpty()) return "NONE";
        // test print:
        // System.out.println("Potential twins found: ");
        // System.out.println(similarity);

        // 2. Pick top 5 users with highest similarity as taste twins
        List<String> twins = new ArrayList<>(similarity.keySet());
        Collections.sort(twins, (a, b) -> {
            double s1 = similarity.get(a);
            double s2 = similarity.get(b);
            if (s1 != s2) return Double.compare(s2, s1);
            return a.compareTo(b);
        });
        twins = twins.subList(0, Math.min(5, twins.size()));
        // test print:
        // System.out.println("Top 5 twins: ");
        // System.out.println(twins);

        // 3. Collect all candidate books from taste twins, excluding books already read by target user
        Set<String> candidateBooks = new HashSet<>();
        for (String twin : twins) {
            Set<String> booksLiked = userToBooks.get(twin);
            if (booksLiked == null) continue;
            for (String book : booksLiked) {
                if (!targetBooks.contains(book)) {
                    candidateBooks.add(book);
                }
            }
        }
        // test print:
        // System.out.println("Candidate books found: ");
        //System.out.println(candidateBooks);

        // 4. Compute scores for each candidate book
        Map<String, Double> bookScore = new HashMap<>();
        for (String book : candidateBooks) {
            int numTwinLikes = 0;
            for (String twin : twins) {
                if (userToBooks.get(twin).contains(book)) {
                    numTwinLikes++;
                }
            }
            int totalUserLikes = bookToUsers.getOrDefault(book, Collections.emptySet()).size();
            double score = (double) numTwinLikes / totalUserLikes;
            bookScore.put(book, score);
        }
        if (bookScore.isEmpty()) return "NONE";
        // test print:
        // System.out.println("Book scores found: ");
        // System.out.println(bookScore);

        // 5. Sort books by their scores
        List<String> books = new ArrayList<>(bookScore.keySet());
        Collections.sort(books, (a, b) -> {
            double s1 = bookScore.get(a);
            double s2 = bookScore.get(b);
            if (s1 != s2) return Double.compare(s2, s1); // descending
            return a.compareTo(b); // alphabetical
        });
        
        int limit = Math.min(5, books.size());
        return String.join(",", books.subList(0, limit));
    }

    // Part 5. Genre Hopper
    public static String shortestPath(
        Map<String, Map<String, Integer>> bookToBookGraph,
        String sourceBook,
        String targetBook) {
        // if source or target is not in the graph, no path exists
        if (!bookToBookGraph.containsKey(sourceBook) || !bookToBookGraph.containsKey(targetBook)) {
            return "NONE";
        }
        // trivial case
        if (sourceBook.equals(targetBook)) {
            return sourceBook;
        }

        // 1. Collect all unique (undirected) edge weights
        List<Integer> allWeights = new ArrayList<>();
        Set<String> seenEdges = new HashSet<>();
        for (Map.Entry<String, Map<String, Integer>> entry : bookToBookGraph.entrySet()) {
            String a = entry.getKey();
            for (Map.Entry<String, Integer> e : entry.getValue().entrySet()) {
                String b = e.getKey();
                String key = (a.compareTo(b) < 0) ? (a + "|" + b) : (b + "|" + a);
                if (!seenEdges.contains(key)) {
                    seenEdges.add(key);
                    allWeights.add(e.getValue());
                }
            }
        }

        if (allWeights.isEmpty()) {
            return "NONE";
        }

        // 2. Compute the median edge weight (double, following reference logic)
        Collections.sort(allWeights);
        int n = allWeights.size();
        double median;
        if (n % 2 == 0) {
            median = (allWeights.get(n / 2 - 1) + allWeights.get(n / 2)) / 2.0;
        } else {
            median = allWeights.get(n / 2);
        }

        // 3. BFS on the filtered graph (only edges with weight >= median)
        Map<String, String> parent = new HashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(sourceBook);
        parent.put(sourceBook, null);

        while (!queue.isEmpty()) {
            String current = queue.remove();
            Map<String, Integer> neighbors = bookToBookGraph.get(current);
            if (neighbors == null) continue;

            List<String> neighborList = new ArrayList<>(neighbors.keySet());
            Collections.sort(neighborList); // alphabetical BFS tie-breaking

            for (String neighbor : neighborList) {
                int weight = neighbors.get(neighbor);
                if (weight < median) {
                    continue;
                }
                if (parent.containsKey(neighbor)) {
                    continue;
                }
                parent.put(neighbor, current);
                if (neighbor.equals(targetBook)) {
                    // reconstruct path immediately
                    List<String> path = new ArrayList<>();
                    String curr = targetBook;
                    while (curr != null) {
                        path.add(curr);
                        curr = parent.get(curr);
                    }
                    Collections.reverse(path);
                    return String.join("->", path);
                }
                queue.add(neighbor);
            }
        }

        return "NONE";
    }
}

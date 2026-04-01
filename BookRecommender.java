import java.util.*;
import java.io.*;

public class BookRecommender {

    public static void main(String[] args) throws Exception {

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
            // System.out.println(bookId);
            String result = singleBookRecommend(bookToBookGraph, bookId);
            System.out.println(result);
        } else if (command.equals("like_history_mn")) {
            if (args.length < 3) return;
            String[] inputBooks = Arrays.copyOfRange(args, 2, args.length);
            // System.out.println(Arrays.toString(inputBooks));
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
        }
        // if the command is invalid, return an error message
        else {System.out.println("Invalid command");}
        return;
    }

    // Part 0: Parsing the CSV file
    // example: userToBooks = {(node)user1: {(edge)book1, (edge)book2, ...}, ...}
    // * user based graph (later will create a book based graph in Part 3)
    public static Map<String, Set<String>> parseCSV(String csvFile) throws Exception {
        Map<String, Set<String>> userToBooks = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(csvFile));
        String line;
        while ((line = br.readLine()) != null) {
            String[] contents = line.split(",");
            String user = contents[0];
            String book = contents[1];
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
        for (String user : bookMap.keySet()) {
            List<String> books = new ArrayList<>(bookMap.get(user));
            // iterate through all books (pair by pair)
            for (int i = 0; i < books.size() - 1; i++) {
                for (int j = i + 1; j < books.size(); j++) {
                    // get the book names (in this pair)
                    String book1 = books.get(i);
                    String book2 = books.get(j);
                    // if meet a new book (put into graph)
                    graph.putIfAbsent(book1, new HashMap<>());
                    graph.putIfAbsent(book2, new HashMap<>());
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
        if (!graph.containsKey(book)) {
            return "NONE";
        }
        Map<String, Integer> neighbors = graph.get(book);
        List<String> books = new ArrayList<>(neighbors.keySet());
        
        // sort the books
        Collections.sort(books, (a, b) -> {
            int weightA = neighbors.get(a);
            int weightB = neighbors.get(b);
            if (weightA != weightB) {
                return weightB - weightA; // descending
            }
            return a.compareTo(b); // alphabetical
        });
        if (books.size() == 0) {
            return "NONE";
        }
        // set the limit numbers of recommended books
        int limit = Math.min(5, books.size());
        // return in a string format
        return String.join(",", books.subList(0, limit));
    }

    // Part 2b: Like-History Nearest Neighbors
    public static String likeHistoryRecommend(Map<String, Map<String, Integer>> bookMap, String[] inputBooks) {
        // compute the total weight of each book
        Map<String, Integer> totalWeight = new HashMap<>();
        // create a set of input books
        Set<String> inputSet = new HashSet<>(Arrays.asList(inputBooks));
        // iterate through all input books
        for (String book : inputSet) {
            // if the book is not in the book map, continue
            if (!bookMap.containsKey(book)) {
                continue;
            }
            // get the neighbors of the book
            Map<String, Integer> neighbors = bookMap.get(book);
            // iterate through all neighbors
            for (String neighbor : neighbors.keySet()) {
                // if the neighbor is in the input set, continue
                if (inputSet.contains(neighbor)) continue;
                // else, update the total weight
                totalWeight.put(neighbor, totalWeight.getOrDefault(neighbor, 0) + 1);
            }
        }
        // System.out.println(totalWeight);
        // if there are no total weight, return NONE
        if (totalWeight.size() == 0) {
            return "NONE";
        }
        // sort the books by their total weight
        List<String> books = new ArrayList<>(totalWeight.keySet());
        Collections.sort(books, (a, b) -> {
            int weightA = totalWeight.get(a);
            int weightB = totalWeight.get(b);
            if (weightA != weightB) {
                return weightB - weightA; // descending
            }
            return a.compareTo(b); // alphabetical
        });
        // set the limit numbers of recommended books
        int limit = Math.min(5, books.size());
        // return in a string format
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

        // 3. Collect all candidate books from taste twins
        Set<String> candidateBooks = new HashSet<>();
        for (String twin : twins) {
            candidateBooks.addAll(userToBooks.get(twin));
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
            int totalUserLikes = bookToUsers.get(book).size();
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
        // 1. Make Filtered-Co-Like graph
        // track the weights of each edge
        List<Integer> weights = new ArrayList<>();
        for (String book : bookToBookGraph.keySet()) {
            for (Integer w : bookToBookGraph.get(book).values()) {
                weights.add(w);
            }
        }
        // if there are no weights, return NONE
        if (weights.isEmpty()) return "NONE";
        Collections.sort(weights);
        int medianWeight = weights.get(weights.size() / 2);
        // build the filtered-co-like graph
        // example: filtered = {(node)book1: {(edge)book2, (edge)book3, ...}, ...}
        // where each book has a list of neighbors with weight >= medianWeight
        Map<String, List<String>> filtered = new HashMap<>();
        for (String book : bookToBookGraph.keySet()) {
            filtered.put(book, new ArrayList<>());
            for (String neighbor : bookToBookGraph.get(book).keySet()) {
                int weight = bookToBookGraph.get(book).get(neighbor);
                if (weight >= medianWeight) {
                    filtered.get(book).add(neighbor);
                }
            }
        }
        // 2. Run BFS
        Queue<String> q = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        Map<String,String> parent = new HashMap<>();
        q.add(sourceBook);
        visited.add(sourceBook);
        while (!q.isEmpty()) {
            String curr = q.remove();
            if (curr.equals(targetBook)) break;
            for (String neighbor : filtered.get(curr)){
                if (!visited.contains(neighbor)){
                    visited.add(neighbor);
                    parent.put(neighbor, curr);
                    q.add(neighbor);
                }
            }
        }
        // 3. Check if there is a path
        if (!parent.containsKey(targetBook)) return "NONE";
        // 4. Reconstruct the path
        List<String> path = new ArrayList<>();
        String current = targetBook;
        while (current != null) {
            path.add(current);
            current = parent.get(current);
        }
        // 5. reverse the path
        Collections.reverse(path);
        // 6. return the path
        return String.join("->", path);

    }
}

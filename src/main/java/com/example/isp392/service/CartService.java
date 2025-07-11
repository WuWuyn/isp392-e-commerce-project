package com.example.isp392.service;

import com.example.isp392.model.Book;
import com.example.isp392.model.Cart;
import com.example.isp392.model.CartItem;
import com.example.isp392.model.Order;
import com.example.isp392.model.OrderItem;
import com.example.isp392.model.User;
import com.example.isp392.repository.CartRepository;
import com.example.isp392.repository.CartItemRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;

    private final BookService bookService;

    private final UserService userService;

    private final CartItemRepository cartItemRepository;

    public CartService(CartItemRepository cartItemRepository, CartRepository cartRepository, BookService bookService, UserService userService) {
        this.cartItemRepository = cartItemRepository;
        this.cartRepository = cartRepository;
        this.bookService = bookService;
        this.userService = userService;
    }

    public Cart getCartForUser(User user) {
        Optional<Cart> optionalCart = cartRepository.findByUser(user);
        if (optionalCart.isPresent()) {
            return optionalCart.get();
        }
        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    public Cart getCartForCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.getUserByUsername(username);
        if (currentUser != null) {
            return getCartForUser(currentUser);
        }
        return new Cart(); // Return an empty cart if user not found or not authenticated
    }

    /**
     * Calculate the total value of all items in the user's cart
     * @param user the user whose cart total to calculate
     * @return the total value as BigDecimal
     */
    public BigDecimal getCartTotal(User user) {
        Cart cart = getCartForUser(user);
        BigDecimal total = BigDecimal.ZERO;
        
        for (CartItem item : cart.getItems()) {
            if (item.getBook() != null && item.getBook().getSellingPrice() != null) {
                BigDecimal itemPrice = item.getBook().getSellingPrice();
                int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                
                total = total.add(itemPrice.multiply(BigDecimal.valueOf(quantity)));
            }
        }
        
        return total;
    }

    /**
     * Kiểm tra số lượng sách trong kho
     * @param bookId ID của sách cần kiểm tra
     * @param requestedQuantity Số lượng sách yêu cầu
     * @return true nếu đủ số lượng, false nếu không đủ
     * @throws RuntimeException nếu sách không tồn tại
     */
    public boolean checkBookAvailability(Integer bookId, int requestedQuantity) {
        Book book = bookService.getBookById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found for id " + bookId));
        
        return book.getStockQuantity() != null && book.getStockQuantity() >= requestedQuantity;
    }

    /**
     * Kiểm tra sản phẩm đã có trong giỏ hàng với số lượng mới có vượt quá số lượng trong kho không
     * @param user Người dùng
     * @param bookId ID của sách
     * @param newQuantity Số lượng mới muốn thêm vào
     * @return true nếu số lượng hợp lệ, false nếu vượt quá kho
     */
    public boolean checkCartItemAvailability(User user, Integer bookId, int newQuantity) {
        Book book = bookService.getBookById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found for id " + bookId));
                
        // Kiểm tra số lượng hiện có trong giỏ hàng
        Cart cart = getCartForUser(user);
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getBook().getBook_id().equals(bookId))
                .findFirst();
                
        int currentQuantity = existingItem.map(CartItem::getQuantity).orElse(0);
        int totalRequestedQuantity = currentQuantity + newQuantity;
        
        return book.getStockQuantity() != null && book.getStockQuantity() >= totalRequestedQuantity;
    }

    public void addBookToCart(User user, Integer bookId, int quantity) {
        Cart cart = getCartForUser(user);
        Book book = bookService.getBookById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found for id " + bookId));
                
        // Kiểm tra số lượng sách trong kho
        if (book.getStockQuantity() == null || book.getStockQuantity() <= 0) {
            throw new RuntimeException("Sách đã hết hàng");
        }
        
        Optional<CartItem> existingItemOptional = cart.getItems().stream()
                .filter(item -> item.getBook().getBook_id().equals(bookId))
                .findFirst();
                
        if (existingItemOptional.isPresent()) {
            CartItem item = existingItemOptional.get();
            int newQuantity = item.getQuantity() + quantity;
            
            // Kiểm tra số lượng mới có vượt quá số lượng trong kho không
            if (newQuantity > book.getStockQuantity()) {
                throw new RuntimeException("Số lượng sách yêu cầu vượt quá số lượng hiện có (" + book.getStockQuantity() + ")");
            }
            
            item.setQuantity(newQuantity);
        } else {
            // Kiểm tra số lượng yêu cầu có vượt quá số lượng trong kho không
            if (quantity > book.getStockQuantity()) {
                throw new RuntimeException("Số lượng sách yêu cầu vượt quá số lượng hiện có (" + book.getStockQuantity() + ")");
            }
            
            CartItem item = new CartItem();
            item.setBook(book);
            item.setQuantity(quantity);
            item.setCart(cart);
            cart.getItems().add(item);
        }
        cartRepository.save(cart);
    }

    public void updateQuantity(User user, Integer bookId, int quantity) {
        Cart cart = getCartForUser(user);
        
        // Kiểm tra số lượng sách trong kho
        Book book = bookService.getBookById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found for id " + bookId));
                
        if (book.getStockQuantity() == null || book.getStockQuantity() <= 0) {
            throw new RuntimeException("Sách đã hết hàng");
        }
        
        if (quantity > book.getStockQuantity()) {
            throw new RuntimeException("Số lượng sách yêu cầu vượt quá số lượng hiện có (" + book.getStockQuantity() + ")");
        }
        
        cart.getItems().forEach(item -> {
            if (item.getBook().getBook_id().equals(bookId)) {
                item.setQuantity(quantity);
            }
        });
        cartRepository.save(cart);
    }

    public void removeItem(User user, Integer bookId) {
        Cart cart = getCartForUser(user);
        cart.getItems().removeIf(item -> item.getBook().getBook_id().equals(bookId));
        cartRepository.save(cart);
    }

    public void clearItems(User user) {
        Cart cart = getCartForUser(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    public List<CartItem> getSelectedCartItems(User user, String[] bookIds) {
        try {
            List<Integer> bookIdList = Arrays.stream(bookIds)
                    .filter(StringUtils::hasText)
                    .map(id -> {
                        try {
                            return Integer.parseInt(id.trim());
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .filter(id -> id != null)
                    .toList();
            
            if (bookIdList.isEmpty()) {
                return new ArrayList<>();
            }
            
            return cartItemRepository.findByCartUserAndBookIds(user, bookIdList);
        } catch (Exception e) {
            // Log the error
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get cart for a user
     * @param user the user whose cart to get
     * @return the user's cart
     */
    public Cart getCart(User user) {
        return getCartForUser(user);
    }

    /**
     * Transfer items from cart to order
     * @param cart the source cart
     * @param order the target order
     */
    public void transferCartItemsToOrder(Cart cart, Order order) {
        for (CartItem cartItem : cart.getItems()) {
            Book book = cartItem.getBook();
            
            // Kiểm tra số lượng sách trong kho trước khi chuyển sang đơn hàng
            if (book.getStockQuantity() == null || book.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + book.getTitle() + " không đủ số lượng trong kho");
            }
            
            // Trừ số lượng sách trong kho
            book.setStockQuantity(book.getStockQuantity() - cartItem.getQuantity());
            
            OrderItem orderItem = new OrderItem();
            orderItem.setBook(book);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(book.getSellingPrice());
            orderItem.setOrder(order);
            order.getOrderItems().add(orderItem);
        }
    }

    /**
     * Clear all items from a cart
     * @param cart the cart to clear
     */
    public void clearCart(Cart cart) {
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}

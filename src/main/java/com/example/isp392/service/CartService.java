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
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private CartItemRepository cartItemRepository;

    public CartService(CartRepository cartRepository, BookService bookService, UserService userService) {
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

    public void addBookToCart(User user, Integer bookId, int quantity) {
        Cart cart = getCartForUser(user);
        Book book = bookService.getBookById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found for id " + bookId));
        Optional<CartItem> existingItemOptional = cart.getItems().stream()
                .filter(item -> item.getBook().getBook_id().equals(bookId))
                .findFirst();
        if (existingItemOptional.isPresent()) {
            CartItem item = existingItemOptional.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
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
            OrderItem orderItem = new OrderItem();
            orderItem.setBook(cartItem.getBook());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getBook().getSellingPrice());
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

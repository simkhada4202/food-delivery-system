package cm.ex.delivery.service;

import cm.ex.delivery.entity.Authority;
import cm.ex.delivery.entity.Image;
import cm.ex.delivery.entity.Restaurant;
import cm.ex.delivery.entity.User;
import cm.ex.delivery.repository.AuthorityRepository;
import cm.ex.delivery.repository.RestaurantRepository;
import cm.ex.delivery.repository.UserRepository;
import cm.ex.delivery.response.BasicResponse;
import cm.ex.delivery.security.authentication.UserAuth;
import cm.ex.delivery.service.interfaces.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantServiceImpl restaurantService;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private AuthorityServiceImpl authorityService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ImageServiceImpl imageService;

    @Autowired
    private BasketServiceImpl basketService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Override
    public BasicResponse signUp(User user, MultipartFile profileImage) throws IOException {
        Optional<User> userEmail = userRepository.findByEmail(user.getEmail());
        if (userEmail.isEmpty())
            return BasicResponse.builder().status(false).code(409).message("This email is already in use").build();
        Set<Authority> authorityList = authorityRepository.findByAuthority("user")
                .map(Set::of)
                .orElse(Set.of());
        user.setId(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setAuthoritySet(authorityList);

        String path = "http://localhost:8080/image/";
        user.setProfileUrl(path + "dummy");
        if (!Objects.requireNonNull(profileImage.getOriginalFilename()).isEmpty()) {
            Image iconImage = imageService.addImage(profileImage);
            user.setProfileUrl(path + iconImage.getId());
        }
        User newUser = userRepository.save(user);
        basketService.createBasket(newUser);
        return BasicResponse.builder().status(true).code(200).message("Account created successfully").build();
    }

    @Override
    public BasicResponse logIn(User user) {
        Optional<User> userEmail = userRepository.findByEmail(user.getEmail());
        if (userEmail.isEmpty() || !passwordEncoder.matches(user.getPassword(), userEmail.get().getPassword()))
            return BasicResponse.builder().status(false).code(401).message("Email or password doesn't match").build();

        UserAuth userAuth = new UserAuth(user.getId().toString(), true, user.getEmail(), user.getPassword(), null, user.getPassword(), convertToGrantedAuthorities(user.getAuthoritySet()), null);
        String jwtToken = jwtService.generateToken(userAuth);

        return BasicResponse.builder().status(true).code(200).message("Successfully logged in").token(jwtToken).build();
    }

    @Override
    public User userInfo() {
        UserAuth userAuth = (UserAuth) SecurityContextHolder.getContext().getAuthentication();
        User user = userAuth.getUser();
        //remove password from user
        return User.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .profileUrl(user.getProfileUrl())
                .authoritySet(user.getAuthoritySet())
                .build();
    }

    @Override
    public List<User> listUserByAuthority(String authority) {
        UserAuth userAuth = (UserAuth) SecurityContextHolder.getContext().getAuthentication();

        Set<Authority> authoritySet = userAuth.getUser().getAuthoritySet();
        Authority highestLevelAuthority = authoritySet.stream()
                .max(Comparator.comparingInt(Authority::getLevel))
                .orElse(null);
        int userHighestLevel = highestLevelAuthority == null ? 0 : highestLevelAuthority.getLevel();

        Optional<Authority> requestedAuthority = authorityRepository.findByAuthority(authority);
        if (requestedAuthority.isEmpty()) throw new NoSuchElementException("Authority not found");
        int requestedAuthorityLevel = requestedAuthority.get().getLevel();

        if (userHighestLevel <= requestedAuthorityLevel) throw new AccessDeniedException("Not enough authority level");

        List<User> userList = userRepository.listByUserAuthority(authority);

        //remove password from user
        List<User> newUserList = userList.stream()
                .map(user -> {
                    return User.builder()
                            .id(user.getId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .profileUrl(user.getProfileUrl())
                            .authoritySet(user.getAuthoritySet())
                            .build();
                })
                .toList();

        return newUserList.isEmpty() ? List.of() : userList;
    }

    @Override
    public BasicResponse assignAuthority(String authority, String userId) {
        UserAuth userAuth = (UserAuth) SecurityContextHolder.getContext().getAuthentication();
        Optional<User> user = userRepository.findById(UUID.fromString(userId));
        if (user.isEmpty()) throw new UsernameNotFoundException("User not found");

        Authority newAuthority = checkForAuthorityUpdate(authority);

        if (authority.equalsIgnoreCase("staff")) {
            Restaurant restaurant = restaurantService.getRestaurantByOwnerId(userAuth.getUser());
            Set<User> staffSet = restaurant.getStaffSet();
            staffSet.add(user.get());
            restaurant.setStaffSet(staffSet);
            restaurantRepository.save(restaurant);
        }

        Set<Authority> authoritySet = user.get().getAuthoritySet();
        authoritySet.add(newAuthority);
        user.get().setAuthoritySet(authoritySet);

        userRepository.save(user.get());
        return BasicResponse.builder().status(true).code(200).message("Authority assigned successfully").build();
    }

    @Override
    public BasicResponse removeAuthority(String authority, String userId) {
        UserAuth userAuth = (UserAuth) SecurityContextHolder.getContext().getAuthentication();
        Optional<User> user = userRepository.findById(UUID.fromString(userId));
        if (user.isEmpty()) throw new UsernameNotFoundException("User not found");

        Authority newAuthority = checkForAuthorityUpdate(authority);

        if (authority.equalsIgnoreCase("staff")) {
            Restaurant restaurant = restaurantService.getRestaurantByOwnerId(userAuth.getUser());
            Set<User> staffSet = restaurant.getStaffSet();
            staffSet.remove(user.get());
            restaurant.setStaffSet(staffSet);
            restaurantRepository.save(restaurant);
        }

        Set<Authority> authoritySet = user.get().getAuthoritySet();
        authoritySet.remove(newAuthority);
        user.get().setAuthoritySet(authoritySet);

        userRepository.save(user.get());
        return BasicResponse.builder().status(true).code(200).message("Authority removed successfully").build();
    }

    @Override
    public List<User> userList() {
        List<User> userList = userRepository.findAll();
        userList = userList.stream().map(user -> {
            return User.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .profileUrl(user.getProfileUrl())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .authoritySet(user.getAuthoritySet())
                    .build();
        }).toList();
        return userList.isEmpty() ? List.of() : userList;
    }

    @Override
    public BasicResponse updateUser(User user) {

        Optional<User> updateUser = userRepository.findById(user.getId());
        if (updateUser.isEmpty())
            return BasicResponse.builder().status(false).code(409).message("Account not found").build();

        updateUser.get().setName(user.getName());
        updateUser.get().setEmail(user.getEmail());
        updateUser.get().setPassword(passwordEncoder.encode(user.getPassword()));
        updateUser.get().setProfileUrl(user.getProfileUrl());

//        when profile image is added
//        if (!Objects.requireNonNull(profileImage.getOriginalFilename()).isEmpty()) {
//            imageService.removeImage(extractImageId(user.getProfileUrl()));
//            Image iconImage = imageService.addImage(profileImage);
//            user.setProfileUrl("http://localhost:8080/image/" + iconImage.getId());
//        }

        userRepository.save(updateUser.get());
        return BasicResponse.builder().status(true).code(200).message("User account updated successfully").build();
    }

//    @Override
//    public BasicResponse updateAddUserAuthority(String newAuthority) {
//        UserAuth userAuth = (UserAuth) SecurityContextHolder.getContext().getAuthentication();
//        Set<Authority> authoritySet = userAuth.getUser().getAuthoritySet();
//        Optional<Authority> checkAuthority = authorityRepository.findByAuthority(newAuthority);
//        if (checkAuthority.isEmpty())
//            return BasicResponse.builder().code(401).result(false).status(false).message("no authority").build();
//

    /// /        if (newAuthority.equalsIgnoreCase("moderator")) {
    /// /            if (!authoritySet.contains(new Authority("admin"))) {
    /// /                throw new AccessDeniedException("No authority");
    /// /            }
    /// /        }
    /// /
    /// /        if (newAuthority.equalsIgnoreCase("delivery")) {
    /// /            if (!authoritySet.contains(new Authority("admin"))) {
    /// /                throw new AccessDeniedException("No authority");
    /// /            }
    /// /        }
    /// /
    /// /        if (newAuthority.equalsIgnoreCase("staff")) {
    /// /            if (!authoritySet.contains(new Authority("owner"))) {
    /// /                throw new AccessDeniedException("No authority");
    /// /            }
    /// /        }
    /// /
    /// /        if (newAuthority.equalsIgnoreCase("admin")) {
    /// /            throw new AccessDeniedException("No authority");
    /// /        }
//
//        Authority authority = authorityService.addAuthority(newAuthority);
//
//        return BasicResponse.builder().code(200).result(true).status(true).message("Authority updated successfully").build();
//    }
//
//    @Override
//    public BasicResponse updateRemoveUserAuthority(String newAuthority) {
//        return null;
//    }
    @Override
    public BasicResponse deleteUser() {
        UserAuth userAuth = (UserAuth) SecurityContextHolder.getContext().getAuthentication();
        basketService.removeBasket(userAuth.getUser());
        userRepository.delete(userAuth.getUser());
        return BasicResponse.builder().status(true).code(200).message("User account deleted successfully").build();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }
        return org.springframework.security.core.userdetails.User.withUsername(user.get().getEmail())
                .password(user.get().getPassword())
                .username(user.get().getEmail())
                .authorities(convertToGrantedAuthorities(user.get().getAuthoritySet()))
                .build();
    }

    private List<GrantedAuthority> convertToGrantedAuthorities(Set<Authority> authorities) {
        return authorities.stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
                .collect(Collectors.toList());
    }

    public static List<String> convertToStringListAuthorities(List<GrantedAuthority> grantedAuthorities) {
        return grantedAuthorities.stream()
                .map(GrantedAuthority::getAuthority) // Extract the authority name
                .collect(Collectors.toList());
    }

    private static String extractImageId(String imageUrl) {
        String uuidRegex = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
        Pattern pattern = Pattern.compile(uuidRegex);
        Matcher matcher = pattern.matcher(imageUrl);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private Authority checkForAuthorityUpdate(String authority) {
        UserAuth userAuth = (UserAuth) SecurityContextHolder.getContext().getAuthentication();
        Optional<Authority> newAuthority = authorityRepository.findByAuthority(authority);
        if (newAuthority.isEmpty()) throw new IllegalArgumentException("Authority cannot be blank.");

        Set<Authority> higherAuthoritySet = userAuth.getUser().getAuthoritySet();
        Optional<Authority> checkAuthority = authorityRepository.findByAuthority(newAuthority.get().getAuthorityAccept());
        if (checkAuthority.isEmpty()) throw new IllegalArgumentException("Authority not found.");

        if (!higherAuthoritySet.contains(checkAuthority.get()))
            throw new AccessDeniedException("Access denied for authority update");
        return newAuthority.get();
    }

}
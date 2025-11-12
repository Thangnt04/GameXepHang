package services;

import models.ClientSession;
import repositories.UserRepository;

public class AuthService {
    private UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ClientSession login(String username, String password) {
        UserRepository.User dbUser = userRepository.login(username, password);
        if (dbUser != null) {
            return new ClientSession(dbUser.username, dbUser.id, 
                dbUser.totalWins, dbUser.totalDraws, dbUser.totalLosses);
        }
        return null;
    }

    public boolean register(String username, String password) {
        return userRepository.register(username, password);
    }
}

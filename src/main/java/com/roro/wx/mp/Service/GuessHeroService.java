package com.roro.wx.mp.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class GuessHeroService {
    @Value("${roconfig.cipher.commit}")
    private String cipherCommitKey;
}

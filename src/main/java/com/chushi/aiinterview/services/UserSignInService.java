package com.chushi.aiinterview.services;

import com.chushi.aiinterview.commons.vo.UserSignInRecordsVo;
import com.chushi.aiinterview.commons.vo.UserSignInStatVo;
import com.chushi.aiinterview.entities.User;

public interface UserSignInService {
    UserSignInStatVo signIn(User currentUser);

    UserSignInStatVo getSignInStat(User currentUser);

    UserSignInRecordsVo getSignInRecords(User currentUser, Integer year);
}

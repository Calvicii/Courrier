package services

import goa.Client
import goa.GoaObject

class AccountService(
    private val client: Client,
) {
    fun getMailAccounts(): List<GoaObject> {
        val accounts = mutableListOf<GoaObject>()
        for (account in client.getAccounts()) {
            if (account.mail !== null) {
                accounts.add(account)
            }
        }
        return accounts
    }
}